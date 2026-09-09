package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.appearance.UniquenessParticleManager;
import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.mcf.AvatarRenderStateAccessor;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Frame-bound particle signatures: their shapes clear with appearance state and cannot accumulate. */
public final class UniquenessAdornmentRenderer {
    private static final TraitGeometry G = TraitGeometry.INSTANCE;
    private UniquenessAdornmentRenderer() {}

    public static void submit(String pathway, PoseStack stack, SubmitNodeCollector collector,
                              AvatarRenderState state, PlayerModel model) {
        if (pathway.equals("death") || pathway.equals("door") || state.distanceToCameraSq > 48 * 48) return;
        float time = state.ageInTicks;
        String uuid = ((AvatarRenderStateAccessor) state).coi$getPlayerUuid();
        int idle = UniquenessParticleManager.idleTicks(uuid);
        String authority = "";
        int moonPhase = 0;
        for (String trait : ClientAppearanceState.getTraits(uuid)) {
            if (trait.startsWith("authority:")) authority = trait.substring(10);
            if (trait.startsWith("moon-phase:") && trait.length() == 12
                    && trait.charAt(11) >= '0' && trait.charAt(11) <= '7') moonPhase = trait.charAt(11) - '0';
        }
        String rule = authority;
        int phase = moonPhase;
        double spacing = .48 * Math.clamp(Math.sqrt(state.distanceToCameraSq) / 8, 1, 3);
        stack.pushPose();
        // Foot signatures stay at the feet when the torso leans, rather than orbiting the back.
        boolean feet = switch (pathway) {
            case "priest", "mother", "hermit", "hanged", "emperor", "darkness", "abyss" -> true;
            default -> false;
        };
        if (!feet) model.body.translateAndRotate(stack);
        collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                (pose, consumer) -> new Motif(pose, consumer, time, spacing, idle, rule, phase).draw(pathway));
        stack.popPose();
        if (pathway.equals("fool") || pathway.equals("chained")) {
            for (var part : pathway.equals("fool") ? new net.minecraft.client.model.geom.ModelPart[]{model.leftLeg, model.rightLeg}
                    : new net.minecraft.client.model.geom.ModelPart[]{model.rightArm}) {
                stack.pushPose();
                part.translateAndRotate(stack);
                collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                        (pose, consumer) -> {
                            Motif motif = new Motif(pose, consumer, time, spacing, idle, rule, phase);
                            if (pathway.equals("fool")) motif.worms(); else motif.armChain();
                        });
                stack.popPose();
            }
        }
    }

    /** Body-local pixels. Signatures occupy short-lived strokes rather than complete solid emblems. */
    private record Motif(PoseStack.Pose pose, VertexConsumer consumer, float time, double spacing, int idle, String authority, int moonPhase) {
        private static final double TAU=Math.PI*2;
        private double cycle(double speed,double phase) { return (time*speed+phase)%1; }
        private double envelope(double t) { return Math.pow(Math.sin(Math.PI*t),2); }
        private void spark(double x,double y,double z,double size,int rgb,double alpha) {
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            float opacity=(float)Math.clamp(Math.sqrt(Math.max(0,alpha))*.8*Math.sqrt(intensity),0,.85);
            if(opacity<.018f)return;
            float r=((rgb>>16)&255)/255f,g=((rgb>>8)&255)/255f,b=(rgb&255)/255f;
            float px=(float)x/16,py=(float)y/16,pz=(float)z/16,half=(float)size/16;
            G.drawBox(pose,consumer,px-half,py-half,pz-half,px+half,py+half,pz+half,r,g,b,opacity*.08f,TraitRenderSupport.FULL_BRIGHT);
            half*=.55f;
            G.drawBox(pose,consumer,px-half,py-half,pz-half,px+half,py+half,pz+half,r,g,b,opacity,TraitRenderSupport.FULL_BRIGHT);
        }
        private void line(double x,double y,double z,double xx,double yy,double zz,int rgb,double alpha) {
            double dx=xx-x,dy=yy-y,dz=zz-z;
            int n=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy+dz*dz)/spacing));
            for(int i=0;i<=n;i++) {
                double t=i/(double)n;
                spark(x+dx*t,y+dy*t,z+dz*t,.32,rgb,alpha);
            }
        }
        private void panel(double left,double top,double right,double bottom,double near,double far,int rgb,double alpha) {
            if(right-left<.001)return;
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            var tint=new TraitGeometry.Tint(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,
                    (float)(alpha*Math.sqrt(intensity)));
            G.quad(pose,consumer,G.pointPixels((float)left,(float)top,(float)near),
                    G.pointPixels((float)right,(float)top,(float)far),
                    G.pointPixels((float)right,(float)bottom,(float)far),
                    G.pointPixels((float)left,(float)bottom,(float)near),tint,TraitRenderSupport.FULL_BRIGHT);
        }
        private void arc(double x,double y,double z,double rx,double ry,double start,double length,int rgb,double alpha) {
            int n=Math.max(4,(int)(Math.abs(length)*Math.max(rx,ry)/spacing));
            for(int i=0;i<=n;i++) {
                double t=i/(double)n,a=start+t*length;
                spark(x+Math.cos(a)*rx,y+Math.sin(a)*ry,z+Math.sin(a*2+time*.025)*.4,.34,rgb,alpha*Math.sin(Math.PI*t));
            }
        }
        private void draw(String pathway) {
            switch(pathway) {
                case "fool" -> { } case "fortune" -> infinity(); case "chained" -> chains();
                case "error" -> clock(); case "moon" -> moon(); case "mother" -> bloom();
                case "visionary" -> thought(); case "sun" -> sun(); case "giant" -> twilight();
                case "priest" -> war(); case "abyss" -> abyss(); case "darkness" -> night();
                case "demoness" -> mirrors(); case "emperor" -> disorder(); case "hanged" -> sacrifice();
                case "hermit" -> knowledge(); case "justiciar" -> order(); case "paragon" -> assembly();
                case "tower" -> pages(); case "tyrant" -> { } default -> { }
            }
        }
        private void worms() {
            // Roots touch the boot/ankle mesh and follow each leg's actual swing.
            for (int worm=0;worm<3;worm++) {
                double angle=worm*TAU/3;
                for(int i=0;i<15;i++) {
                    double u=i/14.0, curl=time*(.028+worm*.007)+u*5+worm*2;
                    double radius=2.05+u*(.65+Math.sin(curl)*.45);
                    double x=Math.cos(angle)*radius+Math.sin(curl)*u*.35;
                    double y=10.2-u*(3.3+Math.sin(curl*.8)*.7);
                    double z=Math.sin(angle)*radius+Math.cos(curl)*u*.4;
                    spark(x,y,z,.48*(1-u*.6),i%3==0?0xA9ABA2:0xCED8CF,.5*(1-u*.55));
                }
            }
        }

        private void infinity() {
            double head=time*.035;
            for(int i=0;i<90;i++) {
                double trail=i/90.0,a=head-trail*TAU;
                double x=8*Math.cos(a)/(1+Math.sin(a)*Math.sin(a));
                double y=4+7*Math.sin(a)*Math.cos(a)/(1+Math.sin(a)*Math.sin(a));
                spark(x,y,6.3+Math.sin(a)*.8,.38,0xC3D2D3,Math.pow(1-trail,1.6));
            }
        }
        private void chains() {
            double phase=cycle(.0017,0), blend=.5+.5*Math.cos(phase*TAU);
            for(int link=0;link<12;link++) {
                double a=link*TAU/12+time*.006;
                double x=Math.cos(a)*5.2,z=Math.sin(a)*3.1;
                double y=6+Math.sin(a)*(1+4*Math.sin(phase*TAU));
                for(int i=0;i<12;i++) {
                    double t=i*TAU/12, tilt=link%2==0?1:.25;
                    spark(x-Math.sin(a)*Math.cos(t)*1.2,y+Math.sin(t)*tilt,
                            z+Math.cos(a)*Math.cos(t)*1.2+Math.sin(t)*(1-tilt),
                            .27,i%3==0?0x9396AD:0x55586C,.15+blend*.4);
                }
            }
            // A travelling connector draws the wrap from the shoulder toward the torso.
            if(blend<.55) for(int i=0;i<16;i++) {
                double u=i/15.0;
                spark(-5+u*5,1+u*5,2.6,.28,0x85899E,(.55-blend)*.6);
            }
        }

        private void armChain() {
            double blend=.5-.5*Math.cos(cycle(.0017,0)*TAU);
            for(int link=0;link<8;link++) {
                double a=link*TAU/4+time*.012;
                double x=Math.cos(a)*2.25-1,y=1+link*1.15,z=Math.sin(a)*2.25;
                for(int i=0;i<10;i++) {
                    double t=i*TAU/10;
                    spark(x+Math.cos(t)*.55,y+Math.sin(t)*.75,z,.25,0x777B91,blend*.55);
                }
            }
        }
        private void clock() {
            double wobble=Math.sin(time*.02)*.3;
            for(int i=0;i<12;i++) {
                double a=i*TAU/12+wobble,slip=i==2?Math.sin(time*.14)*.8:0;
                double r=6.8+slip,fade=.25+.55*envelope(cycle(.004,i*.083));
                line(Math.sin(a)*r,3-Math.cos(a)*r,6,Math.sin(a)*(r-.65),3-Math.cos(a)*(r-.65),6,i%2==0?0x92949A:0xBA965D,fade);
            }
            arc(0,3,6,6.1,6.1,-time*.012,Math.PI*.9,0x787B83,.5);
            double hand=time*.035+Math.sin(time*.11)*.9;
            line(0,3,6,Math.sin(hand)*5,3-Math.cos(hand)*5,6,0xD8BD7A,.75);
            line(0,3,6,Math.sin(-hand*.31)*3,3-Math.cos(-hand*.31)*3,6,0xB0B1B4,.6);
            for(int i=0;i<5;i++) {
                double t=cycle(.009,i*.2),a=i*1.9-time*.015;
                spark(Math.sin(a)*(7+t*2),3-Math.cos(a)*(7+t*2),6,.28,0xC1A169,envelope(t)*.65);
            }
        }
        private void moon() {
            // Vanilla order: full, waning gibbous/quarter/crescent, new, then waxing.
            double light=Math.cos(moonPhase*Math.PI/4), direction=moonPhase<4?1:-1;
            double radius=4.6, y=2+Math.sin(time*.01)*.3;
            for(double yy=-radius+.2;yy<radius;yy+=.4) {
                double edge=Math.sqrt(Math.max(0,radius*radius-yy*yy));
                double boundary=-edge*light*direction;
                if(direction>0) {
                    panel(-edge,y+yy-.2,boundary,y+yy+.2,5.3,5.3,0x503A50,.22);
                    if(moonPhase!=4)panel(boundary,y+yy-.2,edge,y+yy+.2,5.3,5.3,0xD98491,.52);
                } else {
                    panel(-edge,y+yy-.2,boundary,y+yy+.2,5.3,5.3,0xD98491,.52);
                    panel(boundary,y+yy-.2,edge,y+yy+.2,5.3,5.3,0x503A50,.22);
                }
            }
            if(moonPhase==0) eye(0,y,5.7,3.1,0xF4CCD1,cycle(.0015,0));
        }

        private void bloom() {
            ground("leaf",0x70955B,0xD9B795);
        }

        private void thought() {
            eye(0,1,5.5,5.2,0xD1B26D,cycle(.0014,0));
        }

        private void eye(double x,double y,double z,double radius,int rgb,double life) {
            // A long invisible rest, then an opening, two blinks and an independent searching pupil.
            if(life>.46)return;
            double fade=Math.min(1,Math.min(life/.055,(.46-life)/.065));
            double blink=1-.98*Math.exp(-Math.pow((life-.18)/.009,2))
                    -.98*Math.exp(-Math.pow((life-.34)/.009,2));
            double opening=Math.max(0,fade*blink);
            for(int i=0;i<=28;i++) {
                double u=i/28.0,xx=(u-.5)*radius*2,yy=Math.sin(u*Math.PI)*radius*.38*opening;
                spark(x+xx,y-yy,z,.28,rgb,fade*.55);
                spark(x+xx,y+yy,z,.28,rgb,fade*.55);
            }
            double look=Math.sin(time*.022)*radius*.32,up=Math.cos(time*.014)*radius*.08;
            line(x+look,y+up-radius*.28*opening,z+.12,
                    x+look,y+up+radius*.28*opening,z+.12,rgb,opening*.8);
            double slit=.25*opening;
            line(x+look-slit,y+up,z+.12,x+look,y+up-radius*.28*opening,z+.12,rgb,opening*.55);
            line(x+look+slit,y+up,z+.12,x+look,y+up+radius*.28*opening,z+.12,rgb,opening*.55);
        }
        private void sun() {
            double pulse=.92+.08*Math.sin(time*.025),radius=4.1*pulse;
            for(double y=-radius+.2;y<radius;y+=.4) {
                double edge=Math.sqrt(Math.max(0,radius*radius-y*y));
                double core=Math.sqrt(Math.max(0,radius*radius*.7-y*y));
                panel(-edge,2+y-.2,-core,2+y+.2,5.5,5.5,0xE5AF54,.52*pulse);
                panel(-core,2+y-.2,core,2+y+.2,5.5,5.5,0xF8D991,.58*pulse);
                panel(core,2+y-.2,edge,2+y+.2,5.5,5.5,0xE5AF54,.52*pulse);
            }
            for(int i=0;i<12;i++) {
                double a=i*TAU/12;
                line(Math.cos(a)*(radius+.5),2+Math.sin(a)*(radius+.5),5.5,
                        Math.cos(a)*(radius+1.3),2+Math.sin(a)*(radius+1.3),5.5,0xEBC474,.28*pulse);
            }
        }

        private void twilight() {
            // Dawn Armor's gold protection, resolving briefly into the uniqueness's sword silhouette.
            double life=cycle(.002,0),fade=envelope(life)*.4;
            if(life<.65) {
                double grow=Math.sin(life/.65*Math.PI),w=3.4*grow;
                line(-w,1,4.7,w,1,4.7,0xDAB470,fade);
                line(-w,1,4.7,-w,6,4.7,0xC98F58,fade);
                line(w,1,4.7,w,6,4.7,0xC98F58,fade);
                line(-w,6,4.7,0,9,4.7,0xBD7A50,fade);
                line(w,6,4.7,0,9,4.7,0xBD7A50,fade);
            } else {
                double f=envelope((life-.65)/.35)*.45;
                line(0,-2,4.8,-.7,7,4.8,0xE7BF82,f);
                line(0,-2,4.8,.7,7,4.8,0xE7BF82,f);
                line(-2,7,4.8,2,7,4.8,0xAF774A,f);
                line(0,7,4.8,0,10,4.8,0xBB8F62,f);
            }
        }

        private void war() {
            ground("fire",0xCA5932,0xE9A45D);
        }

        private void abyss() {
            ground("curse",0x726C51,0x67878A);
        }

        private void night() {
            ground("star",0x73809F,0xA7ABC3);
        }

        private void ground(String kind,int rgb,int highlight) {
            double strength=idle<6?.55:.15;
            for(int i=0;i<4;i++) {
                double t=cycle(idle<6?.016:.006,i*.25),a=i*TAU/4+time*.013;
                double x=Math.cos(a)*3.3,z=Math.sin(a)*2.8;
                double y=23.7-t*(idle<6?3.5:1.8),fade=envelope(t)*strength;
                if(kind.equals("thread")||kind.equals("fire")) {
                    for(int j=0;j<9;j++) {
                        double u=j/8.0;
                        spark(x+Math.sin(u*6+time*.035+i)*u*.6,y+u*1.6,z+u*2,.25,j%3==0?highlight:rgb,fade*(1-u*.6));
                    }
                } else if(kind.equals("star")) {
                    line(x-.55,y,z,x+.55,y,z,rgb,fade);
                    line(x,y-.55,z,x,y+.55,z,highlight,fade);
                } else if(kind.equals("leaf")) {
                    line(x,y+1,z,x,y-1,z,rgb,fade);
                    line(x,y,z,x+1,y-.6,z+.3,rgb,fade);
                    spark(x,y-1,z,.35,highlight,fade);
                } else if(kind.equals("order")) {
                    double skew=Math.sin(time*.05+i)*.6;
                    line(x-.6,y-.6,z,x+.6+skew,y-.6,z,rgb,fade);
                    line(x+.6+skew,y-.6,z,x+.6,y+.6,z,highlight,fade);
                    line(x+.6,y+.6,z,x-.6,y+.6,z,rgb,fade);
                } else {
                    line(x,y-.6,z,x,y+.6,z,rgb,fade);
                    line(x,y,z,x+.7,y-.4,z,highlight,fade);
                }
            }
        }
        private void mirrors() {
            for(int i=0;i<4;i++) {
                double t=cycle(.004,i*.25),side=i%2==0?1:-1,x=side*(6+Math.sin(t*TAU)),y=-3+i*4+Math.sin(time*.025+i),z=4+Math.cos(time*.02+i);
                double width=.7+.4*Math.sin(time*.03+i);
                line(x,y-1.7,z,x+width,y,z,0x9EBEC5,envelope(t)*.65);
                line(x+width,y,z,x,y+1.7,z,0x9EBEC5,envelope(t)*.65);
                line(x,y+1.7,z,x-width,y,z,0x716F8B,envelope(t)*.45);
                line(x-width,y,z,x,y-1.7,z,0x716F8B,envelope(t)*.45);
            }
        }
        private void disorder() {
            ground("order",0xAC914F,0x817087);
        }

        private void sacrifice() {
            ground("thread",0x963D4B,0xB96068);
        }

        private void knowledge() {
            ground("rune",0xA58BAE,0xB7A588);
        }

        private void order() {
            if(authority.isEmpty())return;
            double tilt=switch(authority) {
                case "consequence" -> Math.sin(time*.07)*1.8;
                case "balance", "quell_disorder" -> Math.sin(time*.04)*.2;
                case "suppression", "confinement" -> -1.1;
                default -> Math.sin(time*.025)*.65;
            };
            double spread=authority.equals("isolation")?7:5,z=5.3;
            line(0,0,z,0,10,z,0xB49A5D,.4);
            line(-spread,3-tilt,z,spread,3+tilt,z,0xD9BA70,.55);
            for(int side=-1;side<=1;side+=2) {
                double x=side*spread,y=3+side*tilt;
                line(x,y,z,x-1.5,y+3,z,0xC4A365,.4);
                line(x,y,z,x+1.5,y+3,z,0xC4A365,.4);
                arc(x,y+3,z,1.5,.8,0,Math.PI,0xE0C47B,.6);
                if(authority.equals("confinement")||authority.equals("no_teleportation"))
                    line(x-1.5,y+3,z,x+1.5,y+3,z,0xE0C47B,.55);
            }
        }

        private void assembly() {
            // The resource pack's cog/sun logo: separated satellites lock into a central machine.
            double life=cycle(.0018,0), assembled=Math.pow(Math.sin(Math.PI*life),4);
            for(int gear=0;gear<3;gear++) {
                double a=gear*TAU/3+time*.005*(1-assembled),r=gear==0?2.5:1.7;
                double distance=6*(1-assembled)+2.6*assembled;
                double x=Math.cos(a)*distance,y=3+Math.sin(a)*distance,z=5.4;
                double turn=time*.025*(gear%2==0?1:-1);
                arc(x,y,z,r,r,turn,TAU,0xBC995F,.42);
                for(int tooth=0;tooth<8;tooth++) {
                    double b=turn+tooth*TAU/8;
                    line(x+Math.cos(b)*r,y+Math.sin(b)*r,z,
                            x+Math.cos(b)*(r+.55),y+Math.sin(b)*(r+.55),z,0xD5B16B,.45);
                }
                spark(x,y,z,.4,0x9EB7B1,.45);
            }
            if(assembled>.35) {
                arc(0,3,5.3,6.5,6.5,0,TAU,0xCDA264,(assembled-.35)*.45);
                line(-2,9,5.3,2,9,5.3,0xBBA576,assembled*.4);
            }
        }

        private void pages() {
            double open=Math.clamp((idle-12)/30.0,0,1),turn=Math.sin(time*.018)*.15;
            double width=2+2.1*open;
            // Paired page surfaces, a spine and a turning leaf; compact and attached to the back.
            for(int side=-1;side<=1;side+=2) {
                double z=3.1+(1.6-open)+turn*side;
                if(side<0)panel(-width,1,0,7,z,3.1,0xBDBBAA,.42);
                else panel(0,1,width,7,3.1,z,0xC7C3AF,.42);
                line(side*width,1,4,side*width,7,4,0x897551,.5);
            }
            line(0,.7,3.3,0,7.3,3.3,0xA59166,.55);
            double leaf=Math.sin(time*.04);
            line(0,1,3.5,leaf*width,1,4.5,0xD5D1BC,.35);
            line(leaf*width,1,4.5,leaf*width,7,4.5,0xD5D1BC,.35);
            if(open>.8) eye(0,4,5,2.1,0xC5B97B,.06+Math.floorMod(idle-12,160)/160.0*.34);
        }

    }
}
