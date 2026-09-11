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
        if (pathway.equals("death") || pathway.equals("tyrant") || state.distanceToCameraSq > 48 * 48) return;
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
        boolean holder = pathway.equals(UniquenessParticleManager.resolvePathway(uuid, ClientAppearanceState.getTraits(uuid)))
                && dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.shouldRenderUniqueness(uuid);
        var movement = UniquenessParticleManager.movement(uuid, time-(float)Math.floor(time));
        double spacing = .48 * Math.clamp(Math.sqrt(state.distanceToCameraSq) / 8, 1, 3);
        stack.pushPose();
        if(pathway.equals("emperor") && holder) model.head.translateAndRotate(stack);
        else if(!pathway.equals("abyss") && !pathway.equals("door") && !pathway.equals("emperor")) model.body.translateAndRotate(stack);
        collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                (pose, consumer) -> new Motif(pose, consumer, time, spacing, idle, rule, phase, movement, holder).draw(pathway));
        stack.popPose();
        if(holder && ClientAppearanceState.hasTrait(uuid,"ability:"+pathway)) {
            stack.pushPose();
            if(!pathway.equals("emperor"))model.body.translateAndRotate(stack);
            collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                    (pose,consumer) -> new Motif(pose,consumer,time,spacing,idle,rule,phase,movement,false).draw(pathway));
            stack.popPose();
        }
    }

    /** Body-local pixels. Signatures occupy short-lived strokes rather than complete solid emblems. */
    private record Motif(PoseStack.Pose pose, VertexConsumer consumer, float time, double spacing, int idle, String authority, int moonPhase, UniquenessParticleManager.Movement movement, boolean holder) {
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
            if(!holder && !pathway.equals("justiciar")) { abilityAura(pathway); return; }
            switch(pathway) {
                case "door" -> lights(); case "fool" -> worms(); case "fortune" -> infinity(); case "chained" -> chains();
                case "error" -> clock(); case "moon" -> moon(); case "mother" -> bloom();
                case "visionary" -> thought(); case "sun" -> sun(); case "giant" -> twilight();
                case "priest" -> war(); case "abyss" -> abyss(); case "darkness" -> night();
                case "demoness" -> mirrors(); case "emperor" -> disorder(); case "hanged" -> sacrifice();
                case "hermit" -> knowledge(); case "justiciar" -> order(); case "paragon" -> assembly();
                case "tower" -> pages(); case "tyrant" -> { } default -> { }
            }
        }
        private static int mix(int a,int b,float t) {
            int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
            int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
            int blue=(int)((a&255)*(1-t)+(b&255)*t);
            return (r<<16)|(g<<8)|blue;
        }

        private void lights() {
            for(int light=0;light<7;light++) {
                double a=time*.006+light*TAU/7,r=8+Math.sin(time*.002+light)*1.2;
                double x=Math.cos(a)*r,y=3+Math.sin(a*.7+light*.8)*12,z=Math.sin(a)*6.7;
                spark(x,y,z,.58,0xFFF9EE,.65);
                for(int i=1;i<=3;i++) {
                    double trail=a-i*.035;
                    double pastRadius=8+Math.sin((time-i*.035/.006)*.002+light)*1.2;
                    spark(Math.cos(trail)*pastRadius,3+Math.sin(trail*.7+light*.8)*12,
                            Math.sin(trail)*6.7,.24,0xE6E5EE,(1-i/4.0)*.2);
                }
            }
        }

        private static final double[][] WORM_CURVES = {
                {3,3.2,2.3, 3,4,12, -4,-8,11, -10,-9,5},
                {2.5,3,2.3, -2,5,7, -7,-1,12, -10,4,6.5},
                {1.5,6,2.3, -7,13,12, -10,13,11, -7,16,5},
                {2,9,2.3, -5,9,5, -5,14,6, 8,14,4},
                {2.3,5,2.3, 3,0,11.5, 10,-2,9, 8,-11,4},
                {2.5,8,2.3, 3,20,13, 17,1,9, 15,-7,4}
        };

        private void worms() {
            int segments=Math.max(20,(int)(44*.48/spacing));
            float opacity=.78f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
            for(int worm=0;worm<WORM_CURVES.length;worm++) {
                double[] curve=WORM_CURVES[worm];
                var points=new TraitGeometry.Point[segments+1];
                var radii=new float[segments+1];
                for(int i=0;i<=segments;i++) {
                    double u=i/(double)segments,v=1-u,drift=u*u;
                    double x=v*v*v*curve[0]+3*v*v*u*curve[3]+3*v*u*u*curve[6]+u*u*u*curve[9];
                    double y=v*v*v*curve[1]+3*v*v*u*curve[4]+3*v*u*u*curve[7]+u*u*u*curve[10];
                    double z=v*v*v*curve[2]+3*v*v*u*curve[5]+3*v*u*u*curve[8]+u*u*u*curve[11];
                    x+=(Math.sin(time*.006+worm*.9+u*2.4)*.55+movement.sway()*.2)*drift;
                    y+=(Math.sin(time*.004+worm+u*3)*.45+movement.lag()*.12)*drift;
                    z+=Math.sin(time*.005+worm*1.3+u*3)*.6*drift;
                    points[i]=G.pointPixels((float)x,(float)y,(float)z);
                    radii[i]=(float)(.37*(1+Math.sin(Math.PI*u)*.25)*(1-u*.88));
                }
                G.drawTube(pose,consumer,points,radii,5,
                        new TraitGeometry.Tint[]{new TraitGeometry.Tint(.91f,.90f,.80f,opacity)},TraitRenderSupport.FULL_BRIGHT);
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
            for(int link=0;link<12;link++) {
                double a=link*TAU/12+time*.004;
                double radius=8.2+Math.sin(time*.013+link*.5)*.35;
                double x=Math.cos(a)*radius,z=Math.sin(a)*5.6;
                double y=8+Math.sin(a*2+time*.015)*1.1+movement.sway()*Math.sin(a)*.45+movement.lag()*Math.cos(a)*.25;
                for(int i=0;i<18;i++) {
                    double t=i*TAU/18,tilt=link%2==0?1:.25;
                    spark(x-Math.sin(a)*Math.cos(t)*2.1,y+Math.sin(t)*tilt,
                            z+Math.cos(a)*Math.cos(t)*2.1+Math.sin(t)*(1-tilt),.31,
                            i%3==0?0xC1C9C0:0x91A79C,.32+.12*Math.sin(time*.012+link));
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
                    panel(-edge,y+yy-.2,boundary,y+yy+.2,5.3,5.3,0x503A50,.05);
                    if(moonPhase!=4)panel(boundary,y+yy-.2,edge,y+yy+.2,5.3,5.3,0xD98491,.18);
                } else {
                    panel(-edge,y+yy-.2,boundary,y+yy+.2,5.3,5.3,0xD98491,.18);
                    panel(boundary,y+yy-.2,edge,y+yy+.2,5.3,5.3,0x503A50,.05);
                }
            }
            for(int i=0;i<=64;i++) {
                double a=i*TAU/64,xx=Math.cos(a)*radius,yy=Math.sin(a)*radius;
                if(moonPhase!=4 && xx*direction>=-Math.abs(Math.cos(a))*radius*light-.01)
                    spark(xx,y+yy,5.6,.3,0xD77486,.45);
            }
            if(moonPhase!=0 && moonPhase!=4)for(int i=0;i<=32;i++) {
                double yy=-radius+i*radius*2/32,edge=Math.sqrt(Math.max(0,radius*radius-yy*yy));
                spark(-edge*light*direction,y+yy,5.6,.27,0xBC5C76,.35);
            }
            if(moonPhase==0) eye(0,y,5.7,3.1,0xF4CCD1,cycle(.0015,0));
        }

        private void bloom() {
            double bloom=movement.bloom(),drift=Math.sin(time*.004)*.06;
            float opacity=.7f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
            for(int vine=0;vine<2;vine++) {
                double start=vine==0?Math.PI/4:Math.PI*3/4;
                var points=new TraitGeometry.Point[37];
                var radii=new float[37];
                for(int i=0;i<points.length;i++) {
                    double u=i/(double)(points.length-1),a=start+u*TAU*.8+drift;
                    double radius=4.8+u*.55,x=Math.cos(a)*radius+movement.sway()*(.12+u*.18);
                    double y=9+u*12,z=Math.sin(a)*4.2+movement.lag()*(.08+u*.17);
                    points[i]=G.pointPixels((float)x,(float)y,(float)z);radii[i]=.12f;
                    if(i%9==0)line(x,y,z,x+Math.cos(a)*1.1,y-.7,z+Math.sin(a)*.7,0x80975B,.45);
                }
                G.drawTube(pose,consumer,points,radii,4,
                        new TraitGeometry.Tint[]{new TraitGeometry.Tint(.27f,.40f,.23f,opacity)},TraitRenderSupport.FULL_BRIGHT);
                double a=start+drift;
                double x=Math.cos(a)*4.8+movement.sway()*.12;
                double y=9+Math.sin(time*.02+vine)*.12,z=Math.sin(a)*4.2+movement.lag()*.08;
                int petal=mix(0xDED5AE,0x98815A,movement.wilt());
                var tint=new TraitGeometry.Tint(((petal>>16)&255)/255f,((petal>>8)&255)/255f,(petal&255)/255f,opacity);
                for(int leaf=0;leaf<6;leaf++) {
                    double b=leaf*TAU/6+movement.sway()*.04,r=1.45*bloom,droop=(1-bloom)*.7;
                    G.quad(pose,consumer,G.pointPixels((float)x,(float)y,(float)(z+.3)),
                            G.pointPixels((float)(x+Math.cos(b-.35)*r*.65),(float)(y+Math.sin(b-.35)*r*.65+droop*.5),(float)(z+.5)),
                            G.pointPixels((float)(x+Math.cos(b)*r),(float)(y+Math.sin(b)*r+droop),(float)(z+.6)),
                            G.pointPixels((float)(x+Math.cos(b+.35)*r*.65),(float)(y+Math.sin(b+.35)*r*.65+droop*.5),(float)(z+.5)),
                            tint,TraitRenderSupport.FULL_BRIGHT);
                }
                spark(x,y,z+.65,.4,0xCDA657,.65);
                if(movement.wilt()>.05) {
                    double t=cycle(.005,vine*.5);
                    spark(x+Math.sin(t*5)*.6,y+t*4,z+.5,.24,0xB09A69,envelope(t)*movement.wilt()*.4);
                }
            }
        }

        private void thought() {
            eye(0,6,7,4.8,0xC8AE72,cycle(.0014,0));
        }

        private void eye(double x,double y,double z,double radius,int rgb,double life) {
            if(life>.5)return;
            double fade=Math.clamp(Math.min(life/.06,(.5-life)/.07),0,1);
            double blink=1-.995*Math.exp(-Math.pow((life-.2)/.010,2))
                    -.995*Math.exp(-Math.pow((life-.38)/.010,2));
            double opening=Math.max(0,fade*blink);
            for(int i=0;i<=36;i++) {
                double u=i/36.0,xx=(u-.5)*radius*2,yy=Math.sin(u*Math.PI)*radius*.36*opening;
                spark(x+xx,y-yy,z,.26,rgb,fade*.5);
                spark(x+xx,y+yy,z,.26,rgb,fade*.5);
            }
            if(opening<.12)return;
            double look=Math.sin(time*.012)*radius*.3;
            double aperture=Math.sin((look/radius+1)*Math.PI/2)*radius*.36*opening;
            double up=Math.sin(time*.008)*aperture*.16,half=aperture*.58;
            line(x+look,y+up-half,z+.4,x+look,y+up+half,z+.4,rgb,fade*.8);
            line(x+look-.22*opening,y+up,z+.4,x+look,y+up-half,z+.4,rgb,fade*.55);
            line(x+look+.22*opening,y+up,z+.4,x+look,y+up+half,z+.4,rgb,fade*.55);
            line(x+look-.22*opening,y+up,z+.4,x+look,y+up+half,z+.4,rgb,fade*.55);
            line(x+look+.22*opening,y+up,z+.4,x+look,y+up-half,z+.4,rgb,fade*.55);
        }

        private void sun() {
            double breath=1+Math.sin(time*.018)*.04,radius=(7.1+movement.activity()*.45)*breath;
            double turn=time*.003+movement.spin()*.15;
            arc(0,4,7,radius,radius,turn,TAU,0xE9CE80,.42);
            arc(0,4,7.3,radius-.45,radius-.45,-turn,TAU,0xB99553,.35);
            for(int ray=0;ray<12;ray++) {
                double a=ray*TAU/12+turn;
                double length=1.1+movement.activity()*.9;
                line(Math.cos(a)*(radius+.3),4+Math.sin(a)*(radius+.3),7,
                        Math.cos(a)*(radius+length),4+Math.sin(a)*(radius+length),7,0xE7C779,.4);
            }
            for(int arm=0;arm<3;arm++)for(int i=0;i<32;i++) {
                double u=i/31.0,a=arm*TAU/3+turn+u*2.4,r=radius+.5+u*(1.5+movement.activity());
                spark(Math.cos(a)*r,4+Math.sin(a)*r,7.2,.26,0xCEAA65,(1-u)*.25);
            }
        }

        private void twilight() {
            double x=movement.sway()*.12,z=5.8+movement.lag()*.08;
            // Silver shield, with the sword's grip and guard projecting above its rim.
            box(x+1,-4,4.8,x+1.65,0,5.5,0x665348,.8);
            box(x-.3,-.7,4.6,x+3,-.25,5.7,0xB8BDC1,.8);
            spark(x+1.3,-4.2,5.2,.48,0xD1BD8E,.7);
            for(double y=0;y<11;y+=.4) {
                double w=y<7?3.7:3.7*(11-y)/4;
                panel(x-w,y,x+w,y+.4,z,z,0x8896A0,.6);
            }
            line(x-3.7,0,z+.1,x+3.7,0,z+.1,0xD2D9D8,.65);
            line(x-3.7,0,z+.1,x-3.7,7,z+.1,0xD2D9D8,.65);
            line(x+3.7,0,z+.1,x+3.7,7,z+.1,0xD2D9D8,.65);
            line(x-3.7,7,z+.1,x,11,z+.1,0xBEA477,.65);
            line(x+3.7,7,z+.1,x,11,z+.1,0xBEA477,.65);
            line(x,1,z+.2,x,9,z+.2,0xD8C491,.35);
            double glint=cycle(.003,0)*10;
            line(x-2,glint,z+.3,x+2,glint-.7,z+.3,0xE1C88E,.18);

        }

        private void war() {
            for(int side=-1;side<=1;side+=2) {
                double x=side*4.3,z=4.5;
                line(x,-2,z,x,12,z,0x9B7860,.45);
                for(int i=0;i<14;i++) {
                    double u=i/14.0,v=(i+1)/14.0,x0=x+side*(.2+u*3.6),x1=x+side*(.2+v*3.6);
                    double z0=z+Math.sin(time*.018-u*3+side)*(.12+movement.activity()*.35)*u+movement.lag()*u*.4;
                    double z1=z+Math.sin(time*.018-v*3+side)*(.12+movement.activity()*.35)*v+movement.lag()*v*.4;
                    panel(Math.min(x0,x1),-1+(u+v)*.45,Math.max(x0,x1),3.3+(u+v)*.35,
                            side<0?z1:z0,side<0?z0:z1,0x8C2630,.48);
                }
                line(x,-.5,z+.1,x+side*2.4,2,z+.2,0xCB9D5B,.35);
            }
        }

        private void abyss() {
            if(movement.groundGap()<.14) {
                float y=23.8f+movement.groundGap()*16;
                float alpha=.25f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
                for(int i=0;i<28;i++) {
                    double a=i*TAU/28,b=(i+1)*TAU/28;
                    G.addTriangle(pose,consumer,0,y/16,0,(float)Math.cos(a)*5.5f/16,y/16,(float)Math.sin(a)*3.8f/16,
                            (float)Math.cos(b)*5.5f/16,y/16,(float)Math.sin(b)*3.8f/16,.035f,.025f,.05f,alpha,TraitRenderSupport.FULL_BRIGHT);
                }
            }
            for(int limb=0;limb<4;limb++) {
                double life=cycle(.0018,limb*.25),grow=envelope(life)*(.3+movement.proximity()*.7);
                for(int i=0;i<=25;i++) {
                    double u=i/25.0,a=limb*TAU/4+Math.sin(time*.005+u*3)*.35;
                    double r=3+u*(2+movement.proximity()*4)*grow;
                    double y=23.2-u*(3+movement.proximity()*8)*grow;
                    spark(Math.cos(a)*r+movement.sway()*u*.12,y,Math.sin(a)*r,
                            .7*(1-u*.8),i%4==0?0x40344E:0x17131E,.65*grow);
                }
            }
        }

        private void night() {
            for(int veil=0;veil<3;veil++)for(int i=0;i<27;i++) {
                double u=i/26.0,a=time*.003+veil*TAU/3;
                // Keep the ribbons outside the full arm/torso envelope.
                double x=Math.cos(a)*(10+Math.sin(u*4+time*.01)*.3);
                double z=Math.sin(a)*6.5;
                spark(x+movement.sway()*u*.12,-2+u*17,z,.23,0xA4B1C7,Math.sin(u*Math.PI)*.22);
            }
            for(int star=0;star<6;star++) {
                double a=star*TAU/6+time*.002,t=cycle(.0018,star/6.0);
                double x=Math.cos(a)*10,y=-2+Math.sin(a*2)*7,z=Math.sin(a)*6.6,fade=.15+envelope(t)*.35;
                spark(x,y,z,.36,0xD4D5E0,fade);
                line(x-.7,y,z,x+.7,y,z,0xA4B2D1,fade);
                line(x,y-.9,z,x,y+.9,z,0xA4B2D1,fade);
            }
        }

        private void box(double x,double y,double z,double xx,double yy,double zz,int rgb,double alpha) {
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            G.drawBox(pose,consumer,(float)x/16,(float)y/16,(float)z/16,(float)xx/16,(float)yy/16,(float)zz/16,
                    ((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,(float)(alpha*Math.sqrt(intensity)),TraitRenderSupport.FULL_BRIGHT);
        }

        private void abilityAura(String pathway) {
            switch(pathway) {
                case "sun" -> {
                    for(int i=0;i<3;i++) {
                        double t=cycle(.004,i/3.0);
                        arc(0,18-t*14,5.5,5+t*2,1.4,time*.006,TAU,0xE6C476,envelope(t)*.35);
                    }
                }
                case "giant" -> {
                    for(int side=-1;side<=1;side+=2) {
                        double x=side*5.2,fade=.3+.15*Math.sin(time*.02);
                        line(x,0,3,x,12,3,0xBAC8CE,fade);
                        line(x,0,3,side*2,1.5,3,0xD5BC85,fade);
                        line(x,12,3,side*2,10.5,3,0xD5BC85,fade);
                    }
                }
                case "priest", "darkness" -> {
                    int rgb=pathway.equals("priest")?0xC66139:0x827F9C;
                    for(int i=0;i<4;i++)for(int j=0;j<18;j++) {
                        double u=j/17.0,life=cycle(.009,i*.25),side=i%2==0?1:-1;
                        spark(side*(5+Math.sin(u*5+time*.015+i)),22-u*20,3+u,.3,rgb,
                                envelope(life)*Math.sin(u*Math.PI)*.4);
                    }
                }
                case "emperor" -> {
                    for(int i=0;i<4;i++) {
                        double a=i*TAU/4+time*.004,x=Math.cos(a)*5,z=Math.sin(a)*3.5,skew=Math.sin(time*.012+i);
                        line(x-1,22,z,x+1+skew,22,z,0xBFA16A,.4);
                        line(x+1+skew,22,z,x+1,23,z,0x826F88,.4);
                    }
                }
                default -> { }
            }
        }

        private void mirrors() {
            double spread=.25+movement.activity()*.6;
            for(int side=-1;side<=1;side+=2) {
                double x=side*(2+spread),z=6+Math.sin(time*.012+side)*.18;
                panel(x-1.8,1,x+1.8,8,z,z+.2,0xB3BBC4,.16);
                line(x-1.8,1,z,x+1.8,1,z,0xB191A9,.45);
                line(x+1.8,1,z,x+1.8,8,z+.2,0xB191A9,.45);
                line(x-1.8,8,z+.2,x+1.8,8,z+.2,0xB191A9,.45);
                line(x-1.8,1,z,x-1.8,8,z+.2,0xB191A9,.45);
                line(x-1.8,2,z+.3,x+.4,4.3,z+.3,0xDECAD1,.5);
                line(x+.4,4.3,z+.3,x-.9,8,z+.3,0xDECAD1,.5);
                line(x+.4,4.3,z+.3,x+1.8,5.8,z+.3,0xD57591,.45);
            }
        }

        private void disorder() {
            for(int i=0;i<32;i++) {
                double a=i*TAU/32,b=(i+1)*TAU/32,x=Math.cos(a)*5,z=Math.sin(a)*5;
                box(x-.43,-10,z-.43,x+.43,-8.6,z+.43,0x17151A,.85);
                line(x,-8.6,z,Math.cos(b)*5,-8.6,Math.sin(b)*5,0xCEAC55,.7);
                line(x,-10,z,Math.cos(b)*5,-10,Math.sin(b)*5,0xCEAC55,.65);
                if(i%4==0) {
                    line(x,-10,z,x,-12,z,0xD7B865,.65);
                    spark(x,-12,z,.4,0x7F2434,.85);
                    spark(x*1.02,-9.3,z*1.02,.44,0xA53742,.8);
                }
            }
        }

        private void sacrifice() {
            // Inverted cross, thorn knots and a crimson grazing eye.
            box(-.45,-5,5.3,.45,18,6,0x33272A,.8);
            box(-6,9.3,5.3,6,10.1,6,0x33272A,.8);
            line(-.5,-5,6.1,-.5,18,6.1,0x9D7B54,.5);
            line(.5,-5,6.1,.5,18,6.1,0x9D7B54,.5);
            line(-6,9.1,6.1,6,9.1,6.1,0x9D7B54,.5);
            for(int knot=0;knot<4;knot++) {
                double y=-2+knot*5;
                line(-1,y-.5,6.2,1,y+.5,6.2,0x9E5960,.5);
                line(-1,y+.5,6.2,1,y-.5,6.2,0x9E5960,.5);
            }
            eye(0,9.7,6.6,1.4,0xA44850,.25);
            for(int thread=0;thread<3;thread++)for(int i=0;i<30;i++) {
                double u=i/29.0,a=u*TAU*1.8-time*.012+thread*TAU/3;
                spark(Math.cos(a)*(1.2+thread*.3),-4+u*22,6.2+Math.sin(a)*.8,.23,
                        0xA93146,.35*(1-u*.35));
            }
            for(int i=0;i<3;i++) {
                double t=cycle(.006,i/3.0);
                spark(Math.sin(i*2)*.5,17+t*5,6.3,.3*(1-t*.5),0xBD4356,envelope(t)*.5);
            }
        }

        private void knowledge() {
            eye(0,6,7,4.1,0xB49BC9,cycle(.0014,0));
            for(int rune=0;rune<5;rune++) {
                double a=time*.003+rune*TAU/5,x=Math.cos(a)*7,y=6+Math.sin(a)*5;
                double fade=.2+.25*envelope(cycle(.002,rune*.2));
                line(x,y-.6,7,x,y+.7,7,0xAE93BA,fade);
                line(x,y,7,x+.8,y-.3,7,0xC1AB77,fade);
            }
        }

        private void order() {
            if(!holder && authority.isEmpty())return;
            double ruleTilt=switch(authority) {
                case "consequence" -> Math.sin(time*.025)*.2;
                case "suppression", "confinement" -> -.14;
                default -> 0;
            };
            double tilt=ruleTilt+movement.sway()*.06+Math.sin(time*.012)*.015;
            double spread=authority.equals("isolation")?8:6,z=7;
            if(holder)arc(0,4,z+.1,10.5,10.5,time*.001,TAU,0xC8AD68,.25);
            line(0,-5,z,0,15,z,0xD2B778,.55);
            line(-Math.cos(tilt)*spread,2-Math.sin(tilt)*spread,z,
                    Math.cos(tilt)*spread,2+Math.sin(tilt)*spread,z,0xD2B778,.6);
            for(int side=-1;side<=1;side+=2) {
                double x=side*Math.cos(tilt)*spread,y=2+side*Math.sin(tilt)*spread;
                double swing=movement.sway()*.085+Math.sin(time*.02+side)*.025;
                double px=x+Math.sin(swing)*4,py=y+Math.cos(swing)*4;
                line(x,y,z,px-1.8,py,z,0xC4A365,.5);
                line(x,y,z,px+1.8,py,z,0xC4A365,.5);
                arc(px,py,z,1.8,.8,0,Math.PI,0xE0C47B,.65);
                if(authority.equals("confinement")||authority.equals("no_teleportation")) {
                    line(px-1.8,py-.8,z,px+1.8,py-.8,z,0xE0C47B,.65);
                    for(int bar=-1;bar<=1;bar++)line(px+bar,py-.8,z,px+bar,py+.5,z,0xD4AB61,.45);
                }
                if(authority.equals("consequence"))spark(px,py+.25,z+.2,.5,0xBC6856,.55);
            }
        }

        private void assembly() {
            double distance=2.8+movement.activity()*2.5;
            for(int gear=0;gear<3;gear++) {
                double a=gear*TAU/3,r=gear==0?2.3:1.65;
                double x=Math.cos(a)*distance+movement.sway()*.3;
                double y=5+Math.sin(a)*distance+movement.lag()*.25,z=6.5;
                double turn=movement.spin()*(gear%2==0?1:-1)+movement.sway()*.025;
                arc(x,y,z,r,r,turn,TAU,0xBC995F,.42);
                for(int tooth=0;tooth<8;tooth++) {
                    double b=turn+tooth*TAU/8;
                    line(x+Math.cos(b)*r,y+Math.sin(b)*r,z,
                            x+Math.cos(b)*(r+.55),y+Math.sin(b)*(r+.55),z,0xD5B16B,.5);
                }
                spark(x,y,z,.36,0x91B0AD,.4);
            }
        }

        private void pages() {
            double open=Math.clamp((idle-12)/30.0,0,1),width=2+2.4*open;
            double z=4.2+movement.lag()*.1;
            for(int side=-1;side<=1;side+=2) {
                double edge=z+1.5-open*.5;
                if(side<0)panel(-width,1,0,9,edge,z,0xBEB49B,.5);
                else panel(0,1,width,9,z,edge,0xC8BEA5,.5);
                line(side*width,1,edge,side*width,9,edge,0x8E6C49,.6);
                for(int row=0;row<3;row++)
                    line(side*.5,2+row*2,z+.1,side*(width-.5),2+row*2,edge+.1,0x77685B,.25);
            }
            line(0,.7,z,0,9.3,z,0xA68B59,.65);
            double turn=Math.sin(time*.014);
            // Every turning leaf remains behind the independent eye plane.
            double leafZ=z+1.5+Math.cos(time*.014)*.35;
            if(turn<0)panel(turn*width,1,0,9,leafZ,z,0xDED5BE,.25*open);
            else panel(0,1,turn*width,9,z,leafZ,0xDED5BE,.25*open);
            if(open>.85)eye(0,5,8.5,2.3,0xC9B775,.10+Math.floorMod(idle-12,200)/200.0*.32);
        }

    }
}
