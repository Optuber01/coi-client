package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
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
        stack.pushPose();
        model.body.translateAndRotate(stack);
        collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                (pose, consumer) -> new Motif(pose, consumer, time, .48 * Math.clamp(Math.sqrt(state.distanceToCameraSq) / 8, 1, 3)).draw(pathway));
        stack.popPose();
    }

    /** Body-local pixels. Signatures occupy short-lived strokes rather than complete solid emblems. */
    private record Motif(PoseStack.Pose pose, VertexConsumer consumer, float time, double spacing) {
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
        private void arc(double x,double y,double z,double rx,double ry,double start,double length,int rgb,double alpha) {
            int n=Math.max(4,(int)(Math.abs(length)*Math.max(rx,ry)/spacing));
            for(int i=0;i<=n;i++) {
                double t=i/(double)n,a=start+t*length;
                spark(x+Math.cos(a)*rx,y+Math.sin(a)*ry,z+Math.sin(a*2+time*.025)*.4,.34,rgb,alpha*Math.sin(Math.PI*t));
            }
        }
        private void draw(String pathway) {
            switch(pathway) {
                case "fool" -> worms(); case "fortune" -> infinity(); case "chained" -> chains();
                case "error" -> clock(); case "moon" -> moon(); case "mother" -> bloom();
                case "visionary" -> thought(); case "sun" -> sun(); case "giant" -> twilight();
                case "priest" -> war(); case "abyss" -> abyss(); case "darkness" -> night();
                case "demoness" -> mirrors(); case "emperor" -> disorder(); case "hanged" -> sacrifice();
                case "hermit" -> knowledge(); case "justiciar" -> order(); case "paragon" -> assembly();
                case "tower" -> pages(); case "tyrant" -> storm(); default -> { }
            }
        }
        private void worms() {
            for(int worm=0;worm<5;worm++) {
                double phase=worm*.217, life=cycle(.0035,phase), fade=envelope(life);
                double side=worm%2==0?1:-1, root=side*(8.3+worm*.18);
                for(int i=0;i<19;i++) {
                    double t=i/18.0;
                    double curl=time*(.025+worm*.003)+t*(4.5+worm*.55)+worm*2;
                    double x=root+side*(.6+Math.sin(curl)*(.45+t*.75));
                    double y=-3+worm*3+t*(3+worm%3)-Math.cos(curl)*t*1.4;
                    double z=Math.cos(life*TAU+worm)*2.4+Math.sin(t*4+time*.018+worm)*t*1.3;
                    spark(x,y,z,(.52+.12*Math.sin(i*2.2))*(1-t*.58),i%3==0?0xA9ABA2:0xCED8CF,fade*(1-t*.45));
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
            for(int link=0;link<10;link++) {
                double a=link*TAU/10+time*.008;
                double radius=8.2+Math.sin(time*.026+link*.5)*.6;
                double x=Math.cos(a)*radius,z=Math.sin(a)*5.2,y=8+Math.sin(a*2+time*.04)*1.2;
                double fade=.2+.6*envelope(cycle(.004,link*.1));
                for(int i=0;i<14;i++) {
                    double t=i*TAU/14,tilt=link%2==0?1:.28;
                    spark(x-Math.sin(a)*Math.cos(t)*1.9,y+Math.sin(t)*tilt,z+Math.cos(a)*Math.cos(t)*1.9+Math.sin(t)*(1-tilt),.32,0xA7B6AE,fade);
                }
                double drop=cycle(.008,link*.31);
                spark(x,y+drop*3,z,.28,0x70847C,envelope(drop)*.4);
            }
        }
        private void clock() {
            double wobble=Math.sin(time*.02)*.3;
            for(int i=0;i<12;i++) {
                double a=i*TAU/12+wobble,slip=i==2?Math.sin(time*.14)*.8:0;
                double r=6.8+slip,fade=.25+.55*envelope(cycle(.004,i*.083));
                line(Math.sin(a)*r,3-Math.cos(a)*r,6,Math.sin(a)*(r-.65),3-Math.cos(a)*(r-.65),6,0xBA965D,fade);
            }
            arc(0,3,6,6.1,6.1,-time*.012,Math.PI*.9,0x786443,.5);
            double hand=time*.035+Math.sin(time*.11)*.9;
            line(0,3,6,Math.sin(hand)*5,3-Math.cos(hand)*5,6,0xD8BD7A,.75);
            line(0,3,6,Math.sin(-hand*.31)*3,3-Math.cos(-hand*.31)*3,6,0x9A8A66,.6);
            for(int i=0;i<5;i++) {
                double t=cycle(.009,i*.2),a=i*1.9-time*.015;
                spark(Math.sin(a)*(7+t*2),3-Math.cos(a)*(7+t*2),6,.28,0xC1A169,envelope(t)*.65);
            }
        }
        private void moon() {
            double a=time*.01,x=Math.sin(a)*1.3,y=3+Math.cos(a)*.8;
            arc(x,y,6,6.8,6.8,Math.PI*.28+Math.sin(a)*.12,Math.PI*1.4,0xCF4961,.65);
            arc(x+.6,y,6.1,6.1,6.1,Math.PI*.3,Math.PI*1.35,0xEF9DA0,.35);
            for(int i=0;i<9;i++) {
                double t=cycle(.004,i/9.0),angle=t*TAU;
                spark(Math.cos(angle)*8,3+Math.sin(angle)*5,5+Math.sin(angle)*2,.28,0xD77887,envelope(t)*.65);
            }
        }
        private void bloom() {
            for(int plant=0;plant<4;plant++) {
                double t=cycle(.0035,plant*.25),growth=Math.sin(Math.PI*t),side=plant%2==0?-1:1,x=side*(7.1+plant*.35),z=Math.sin(time*.012+plant*1.7)*3;
                double top=15-growth*7,tipX=x+Math.sin(t*4)*.45;
                line(x,15,z,tipX,top,z,0x789F65,envelope(t)*.7);
                for(int leaf=0;leaf<2;leaf++) {
                    double yy=14-leaf*2-growth;
                    line(x,yy,z,x+side*1.4*growth,yy-1,z+.3,0x87B778,envelope(t)*.8);
                    line(x+side*1.4*growth,yy-1,z+.3,x,yy-.6,z,0x789F65,envelope(t)*.65);
                }
                for(int petal=0;petal<5;petal++) {
                    double a=petal*TAU/5+time*.008,r=growth*1.3;
                    line(tipX,top,z,tipX+Math.cos(a)*r,top+Math.sin(a)*r,z+.3, t<.65?0xE4C3A0:0x99835B,envelope(t));
                }
                if(t>.55)spark(tipX+Math.sin(t*8),top+(t-.55)*9,z+.6,.3,0xBFA66C,envelope((t-.55)/.45)*.5);
            }
        }
        private void thought() {
            double opening=.2+.8*envelope(cycle(.003,0));
            // A single narrow draconic gaze, with fading thought ripples; no dragon-shaped billboard.
            for(int i=0;i<=28;i++) {
                double t=i/28.0,x=(t-.5)*12,y=Math.sin(t*Math.PI)*2*opening;
                spark(x,1-y,6,.32,0xBDBDAE,opening*.65);
                spark(x,1+y,6,.32,0xBDBDAE,opening*.65);
            }
            line(0,1-opening*1.5,6.2,0,1+opening*1.5,6.2,0xE3CA81,opening);
            line(-.5*opening,1,6.2,0,1-opening*1.5,6.2,0xD4BB75,opening*.8);
            line(.5*opening,1,6.2,0,1+opening*1.5,6.2,0xD4BB75,opening*.8);
            for(int i=0;i<2;i++) {
                double t=cycle(.004,i*.5);
                arc(0,1,6.3,6+t*2,2+t*2,time*.008,Math.PI*1.4,0x8E9D9E,envelope(t)*.4);
            }
        }
        private void sun() {
            double breath=.5+.5*Math.sin(time*.035),radius=5.8+breath*.55;
            arc(0,3,6,radius,radius,time*.008,TAU*.9,0xF4D995,.55+breath*.25);
            for(int i=0;i<8;i++) {
                double a=i*TAU/8+time*.004,t=cycle(.006,i*.125),r=radius+t*2;
                line(Math.cos(a)*r,3+Math.sin(a)*r,6,Math.cos(a)*(r+.7),3+Math.sin(a)*(r+.7),6,0xE7B451,envelope(t)*.7);
            }
        }
        private void twilight() {
            for(int side=-1;side<=1;side+=2) for(int plate=0;plate<3;plate++) {
                double t=cycle(.003,plate*.25+(side+1)*.1),fade=envelope(t),x=side*(6.8+plate*.3),y=-1+plate*3+t;
                line(x,y,4,x+side*2,y+1,4,0xC18B61,fade*.8);
                line(x+side*2,y+1,4,x+side*1.5,y+3,4,0xA77550,fade*.6);
                line(x+side*1.5,y+3,4,x,y,4,0xD4A170,fade*.7);
                line(x+side*.7,y+1,4,x+side*1.3,y+2,4,0xC9925F,fade*.65);
                spark(x+side*1.5,y+3+t*3,4,.3,0xB17C51,fade*.6);
            }
            arc(0,7,5,7,2.1,0,Math.PI,0xAF7450,.25+.2*Math.sin(time*.02));
        }
        private void war() {
            for(int side=-1;side<=1;side+=2) for(int i=0;i<4;i++) {
                double t=cycle(.012,i*.25),y=13-t*13,x=side*(5.5+Math.sin(t*5+time*.02)*.6);
                line(x,y,4,x+side*Math.sin(t*4)*1.5,y+2,4+Math.sin(time*.05+i),0xC9582E,envelope(t)*.8);
                spark(x,y-.5,4,.4,0xF0AF5E,envelope(t));
            }
        }
        private void abyss() {
            for(int i=0;i<5;i++) {
                double t=cycle(.006,i*.2),side=i%2==0?1:-1,x=side*(5.5+Math.sin(t*5+i)),y=-2+t*17;
                arc(x,y,4,1.1,.9,time*.02+i,Math.PI*1.2,0x817D53,envelope(t)*.7);
                spark(x,y+1.3,4,.35,0x678C91,envelope(t)*.5);
            }
        }
        private void night() {
            for(int veil=0;veil<3;veil++) for(int i=0;i<20;i++) {
                double t=i/19.0,a=time*.012+veil*TAU/3;
                spark(Math.cos(a)*(7.5+Math.sin(t*5+time*.025)*.6),1+t*12,Math.sin(a)*4.5,
                        .3,0x8191AF,Math.sin(t*Math.PI)*(.2+.3*envelope(cycle(.003,veil*.333))));
            }
            for(int i=0;i<7;i++) {
                double t=cycle(.003,i/7.0),a=i*2.4+time*.004,x=Math.cos(a)*7,y=-4+t*17,z=4+Math.sin(a)*2;
                double fade=Math.pow(envelope(t),2);
                spark(x,y,z,.42,0xA6AEC7,fade*.65);
                line(x-.6,y,z,x+.6,y,z,0x7887AB,fade*.45);
                line(x,y-.6,z,x,y+.6,z,0x7887AB,fade*.45);
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
            for(int i=0;i<2;i++) {
                double t=cycle(.004,i*.5),skew=Math.sin(t*TAU)*1.5,side=i==0?-1:1;
                double x=side*7,y=4+Math.cos(time*.012+i)*2,z=Math.sin(time*.012+i*Math.PI)*4;
                line(x-1.5,y-2,z,x+1.5+skew,y-2,z,0xBBA064,.7);
                line(x+1.5+skew,y-2,z,x+1.5,y+2,z,0x8B7795,.6);
                line(x+1.5,y+2,z,x-1.5-skew,y+2,z,0xBBA064,.7);
                line(x-1.5-skew,y+2,z,x-1.5,y-.5,z,0x8B7795,envelope(t)*.7);
            }
        }
        private void sacrifice() {
            for(int i=0;i<4;i++) {
                double t=cycle(.004,i*.25),side=i%2==0?1:-1,fade=envelope(t);
                for(int j=0;j<20;j++) {
                    double u=j/19.0,x=side*(6+u)+Math.sin(u*8+time*.025+i)*.45;
                    spark(x,1+u*19,3+Math.sin(u*5+i+time*.01)*1.5,.36, j%3==0?0xC26868:0xA24853,fade*(1-u*.4));
                    if(j%6==0)line(x-.6,1+u*19,4,x+.6,1+u*19+.4,4,0xB46065,fade*.6);
                }
            }
        }
        private void knowledge() {
            for(int rune=0;rune<4;rune++) {
                double t=cycle(.005,rune*.25),x=Math.cos(time*.01+rune*TAU/4)*8,y=-2+rune*3.5-t*2,z=Math.sin(time*.01+rune*TAU/4)*4;
                double fade=envelope(t);
                line(x,y,z,x,y+1.8,z,0xAA92BA,fade*.8);
                line(x,y+.6,z,x+1.2,y+.1,z,0xAA92BA,fade*.8);
                if(t>.2)line(x,y+1.4,z,x-.8,y+2,z,0xC2AA8C,fade*.6);
            }
        }
        private void order() {
            double t=cycle(.004,0),tilt=Math.sin(t*TAU)*Math.pow(1-t,2)*1.4,z=3+Math.sin(time*.014)*1.2;
            for(int side=-1;side<=1;side+=2) {
                double x=side*8,y=9+side*tilt;
                line(x-1.6,y,z,x+1.6,y,z,0xE0C27B,.85);
                line(x,y-3,z,x-1.6,y,z,0xC3A46B,.65);
                line(x,y-3,z,x+1.6,y,z,0xC3A46B,.65);
                line(x,y-5,z,x,y-3,z,0xD6BC78,.6);
                spark(x,y+.4,z,.4,0xD6BC78,.6);
            }
            line(-8,4-tilt,z,8,4+tilt,z,0xAD9B6D,.18+.15*envelope(t));
        }
        private void assembly() {
            for(int gear=0;gear<2;gear++) {
                double a=time*.012+gear*Math.PI,x=Math.cos(a)*7.8,y=4+Math.sin(a*2)*2,z=Math.sin(a)*4.5;
                double r=gear==0?2.5:2,turn=time*.028*(gear==0?1:-1);
                arc(x,y,z,r,r,turn,TAU*.98,0xC9A369,.75);
                for(int tooth=0;tooth<8;tooth++) {
                    double b=turn+tooth*TAU/8,fade=.3+.5*envelope(cycle(.006,tooth*.125));
                    line(x+Math.cos(b)*r,y+Math.sin(b)*r,z,x+Math.cos(b)*(r+.6),y+Math.sin(b)*(r+.6),z,0xD5B879,fade);
                    if(tooth%2==0)line(x,y,z,x+Math.cos(b)*(r-.4),y+Math.sin(b)*(r-.4),z,0x729FAA,.5);
                }
                spark(x,y,z,.45,0x9CC2C9,.8);
            }
        }
        private void pages() {
            for(int i=0;i<3;i++) {
                double t=cycle(.0035,i*.333),a=time*.007+i*TAU/3,x=Math.cos(a)*8,y=4+Math.sin(a*2)*2,z=Math.sin(a)*4.5;
                double fold=Math.sin(time*.035+i)*1.2,fade=envelope(t);
                line(x-1.5,y-1.3,z+fold,x,y-1.6,z,0xC9C9BC,fade*.7);
                line(x,y-1.6,z,x+1.5,y-1.3,z-fold,0xC9C9BC,fade*.7);
                line(x,y-1.6,z,x,y+1.6,z,0xACB6B1,fade*.6);
                line(x-1.5,y+1.3,z+fold,x,y+1.6,z,0xC9C9BC,fade*.7);
                line(x,y+1.6,z,x+1.5,y+1.3,z-fold,0xC9C9BC,fade*.7);
                line(x-1.5,y-1.3,z+fold,x-1.5,y+1.3,z+fold,0xC9C9BC,fade*.65);
                line(x+1.5,y-1.3,z-fold,x+1.5,y+1.3,z-fold,0xC9C9BC,fade*.65);
            }
        }
        private void storm() {
            for(int band=0;band<2;band++) {
                double head=time*.04+band*Math.PI;
                for(int i=0;i<30;i++) {
                    double t=i/30.0,a=head-t*Math.PI*1.3;
                    spark(Math.cos(a)*7,9+Math.sin(a*2)*1.4,Math.sin(a)*4.5,.35,0x6EA9BC,(1-t)*.7);
                }
            }
            double t=cycle(.008,0);
            if(t<.18) {
                double fade=envelope(t/.18)*.6;
                line(6,0,5,4,3,5,0xBAD5DE,fade);
                line(4,3,5,6,3,5,0xBAD5DE,fade);
                line(6,3,5,5,6,5,0xBAD5DE,fade);
            }
        }
    }
}
