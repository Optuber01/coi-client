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

    /** Coordinates are body-local pixels: positive Z is behind the player, negative Y is up. */
    private record Motif(PoseStack.Pose pose, VertexConsumer consumer, float time, double spacing) {
        private void spark(double x,double y,double z,double size,int rgb,double phase) {
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            float pulse=(float)((.68+.22*Math.sin(time*.06+phase))*Math.sqrt(intensity));
            float red=((rgb>>16)&255)/255f,green=((rgb>>8)&255)/255f,blue=(rgb&255)/255f;
            float px=(float)x/16,py=(float)y/16,pz=(float)z/16,r=(float)size/16;
            // A small bright core with a dim additive envelope reads as light, not solid metal.
            G.drawBox(pose,consumer,px-r,py-r,pz-r,px+r,py+r,pz+r,red,green,blue,pulse*.10f,TraitRenderSupport.FULL_BRIGHT);
            r*=.44f;
            G.drawBox(pose,consumer,px-r,py-r,pz-r,px+r,py+r,pz+r,red,green,blue,pulse*.7f,TraitRenderSupport.FULL_BRIGHT);
        }
        private void box(double x,double y,double z,double w,double h,double d,int rgb) {
            int nx=Math.max(1,(int)Math.ceil(w/(spacing*1.35))),ny=Math.max(1,(int)Math.ceil(h/(spacing*1.35)));
            for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) {
                double phase=i*2.1+j*.7+x;
                double zz=z+Math.sin(time*.035+phase)*Math.min(.35,d*.3);
                spark(x+(i+.5)*w/nx-w/2,y+(j+.5)*h/ny-h/2,zz,Math.min(.35,Math.max(w/nx,h/ny)*.38),rgb,phase);
            }
        }
        private void line(double x,double y,double z,double xx,double yy,double zz,double width,int rgb) {
            double dx=xx-x,dy=yy-y,dz=zz-z,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
            int steps=Math.max(1,(int)Math.ceil(length/spacing));
            double travel=(time*.018)%1;
            for(int i=0;i<steps;i++) {
                double t=(i+travel)/steps;
                spark(x+dx*t,y+dy*t,z+dz*t,Math.max(.22,Math.min(.65,width)),rgb,x+y+i*.7);
            }
        }
        private void facet(double ax,double ay,double bx,double by,double cx,double cy,double z,int rgb) {
            line(ax,ay,z,bx,by,z,.26,rgb);line(bx,by,z,cx,cy,z+.7,.26,rgb);line(cx,cy,z+.7,ax,ay,z,.26,rgb);
            for(int i=1;i<5;i++) {
                double t=i/5.0;
                line(ax+(cx-ax)*t,ay+(cy-ay)*t,z,bx+(cx-bx)*t,by+(cy-by)*t,z,.16,rgb);
            }
        }
        private void blade(double x,double y,double xx,double yy,double width,double z,int dark,int light) {
            double dx=xx-x,dy=yy-y,len=Math.sqrt(dx*dx+dy*dy);
            if(len<.001) return;
            double nx=-dy/len*width,ny=dx/len*width;
            facet(x+nx,y+ny,xx,yy,x,y,z,dark);
            facet(xx,yy,x-nx,y-ny,x,y,z,light);
        }
        private void ring(double x, double y, double z, double rx, double ry, double width, int rgb, double phase) {
            for (int i=0; i<32; i++) {
                double a=i*Math.PI/16+phase,b=(i+1)*Math.PI/16+phase;
                line(x+Math.cos(a)*rx,y+Math.sin(a)*ry,z,x+Math.cos(b)*rx,y+Math.sin(b)*ry,z,width,rgb);
            }
        }
        private void draw(String pathway) {
            switch(pathway) {
                case "fool" -> worms();
                case "fortune" -> serpent();
                case "chained" -> chains();
                case "error" -> clock();
                case "moon" -> moon();
                case "mother" -> tree();
                case "visionary" -> dragon();
                case "sun" -> sun();
                case "giant" -> twilight();
                case "priest" -> war();
                case "abyss" -> abyss();
                case "darkness" -> night();
                case "demoness" -> mirrors();
                case "emperor" -> disorder();
                case "hanged" -> sacrifice();
                case "hermit" -> knowledge();
                case "justiciar" -> scales();
                case "paragon" -> gears();
                case "tower" -> tower();
                case "tyrant" -> storm();
                default -> { }
            }
        }
        private void worms() {
            for (int arm=0;arm<6;arm++) {
                double angle=arm*Math.PI/3;
                double previousX=0,previousY=0,previousZ=0;
                for (int i=0;i<24;i++) {
                    double u=i/23.0, a=angle+u*1.5+.32*Math.sin(time*.035+u*5+arm);
                    double radius=3+10*u;
                    double x=Math.cos(a)*radius,y=3+Math.sin(a)*radius*.85,z=7+Math.sin(u*5+time*.025+arm)*2;
                    double thickness=.8*(1-u*.75);
                    if(i>0) line(previousX,previousY,previousZ,x,y,z,thickness,i%3==0?0x727A8B:0xB6C5CC);
                    box(x,y,z,thickness*1.5,thickness*1.35,thickness*1.6,i%3==0?0x91998D:0xD8E2DF);
                    previousX=x;previousY=y;previousZ=z;
                }
            }
        }
        private void serpent() {
            double headAngle=time*.006;
            for(int i=0;i<64;i++) {
                double a=i*Math.PI/32+headAngle,b=(i+1)*Math.PI/32+headAngle;
                double radius=10+.45*Math.sin(a*3+time*.02);
                double nextRadius=10+.45*Math.sin(b*3+time*.02);
                double thickness=.3+.65*Math.sin((i+3)/67.0*Math.PI);
                line(Math.cos(a)*radius,2+Math.sin(a)*radius,8+Math.sin(a*2)*1.1,
                        Math.cos(b)*nextRadius,2+Math.sin(b)*nextRadius,8+Math.sin(b*2)*1.1,
                        thickness,i%4==0?0x78929A:0xB6CDDE);
            }
            double r=10+.45*Math.sin(headAngle*3+time*.02);
            double x=Math.cos(headAngle)*r,y=2+Math.sin(headAngle)*r,z=8+Math.sin(headAngle*2)*1.1;
            box(x,y,z,2,1.5,1.4,0xCADCE4);
            box(x+.45,y-.45,z+.73,.3,.25,.1,0x263E40);
            line(x-.8,y+.3,z+.75,x+.8,y+.3,z+.75,.09,0x596F70);
        }
        private void chains() {
            for(int link=0;link<12;link++) {
                double a=link*Math.PI/6+Math.sin(time*.025)*.07;
                double x=Math.cos(a)*11.8,z=Math.sin(a)*8.2,y=6+Math.sin(a*2)*2;
                for(int i=0;i<12;i++) {
                    double t=i*Math.PI/6,u=(i+1)*Math.PI/6;
                    double v=link%2==0?1.45:.25;
                    line(x-Math.sin(a)*Math.cos(t)*2.8,y+Math.sin(t)*v,z+Math.cos(a)*Math.cos(t)*2.8+Math.sin(t)*(1-v),
                            x-Math.sin(a)*Math.cos(u)*2.8,y+Math.sin(u)*v,z+Math.cos(a)*Math.cos(u)*2.8+Math.sin(u)*(1-v),.38,i<6?0xBAC5BE:0x485A56);
                }
            }
        }
        private void clock() {
            for(int i=0;i<12;i++) {
                double a=i*Math.PI/6;
                double slip=i==2?1.1:0;
                double x=Math.sin(a)*(10+slip),y=2-Math.cos(a)*10;
                box(x,y,8,1.1,2.1,1.4,i==2?0xECCB85:0x998466);
                if(i!=2 && i!=7) {
                    double b=a+Math.PI/7;
                    line(x,y,8,Math.sin(b)*10,2-Math.cos(b)*10,8,.6,0x8C7759);
                }
            }
            ring(0,2,6.7,7.7,7.7,.35,0x726554,0);
            box(0,2,8,1.8,1.8,2,0xB4A27C);
            double a=Math.floor(time/16)*Math.PI/6;
            if(((int)time%96)>72) a-=Math.PI/3;
            line(0,2,8,Math.sin(a)*7,2-Math.cos(a)*7,8,.3,0xDBC385);
            line(0,2,8,Math.sin(-a*.35)*4,2-Math.cos(-a*.35)*4,8,.4,0x9DABA0);
            box(0,2,8,1,1,1,0xE2D4AD);
        }
        private void crescent(double x,double y,double z,double radius,int rgb) {
            for(int i=0;i<28;i++) {
                double a=(i/27.0*1.55+.225)*Math.PI;
                double b=((i+1)/27.0*1.55+.225)*Math.PI;
                double w=Math.sin(i/28.0*Math.PI)*radius*.16+.12;
                line(x+Math.cos(a)*radius,y+Math.sin(a)*radius,z,x+Math.cos(b)*radius,y+Math.sin(b)*radius,z,w,rgb);
            }
        }
        private void moon() {
            crescent(0,2,8,10,0xC32645);
            crescent(.4,2,8.1,9.6,0xF07882);
            for(int i=0;i<3;i++) {
                double a=time*.009+i*Math.PI*2/3;
                crescent(Math.cos(a)*12,2+Math.sin(a)*12,9+Math.sin(a)*1.5,1.5,0xCB7380);
            }
        }
        private void branch(double x,double y,double dx,double dy,int rgb) {
            double z=8+dx*.12;
            line(x,y,8,x+dx,y+dy,z,.55,rgb);
            line(x+dx*.55,y+dy*.55,z,x+dx*.55-dy*.28,y+dy*.55+dx*.28,z+1,.32,rgb);
            line(x+dx*.75,y+dy*.75,z,x+dx*.75+dy*.25,y+dy*.75-dx*.25,z-1,.28,rgb);
        }
        private void tree() {
            line(-.7,15,8,.2,-8,8,1.0,0x756044);
            line(.8,13,8.2,-.3,-6,8.2,.42,0xAC9060);
            for(int side=-1;side<=1;side+=2) {
                for(int i=0;i<4;i++) {
                    double y=8-i*4,dx=side*(9-i),dy=-4-i*.3,z=8+dx*.12;
                    branch(0,y,dx,dy,0x8E8853);
                    double bloom=.7+.3*Math.sin(time*.025+i);
                    for(int petal=0;petal<5;petal++) {
                        double a=petal*Math.PI*2/5;
                        box(dx+Math.cos(a)*bloom,y+dy+Math.sin(a)*bloom,z,1.2,1.2,.65,
                                i%2==0?0xC6C986:0xE9C29F);
                    }
                    box(dx,y+dy,z+.45,.7,.7,.35,0xEFCB73);
                    line(dx-side*2,y+dy+2,z,dx-side*4,y+dy+1,z,.7,0x527B4D);
                }
                branch(0,11,side*8,5,0x806044);
                branch(0,12,side*4,7,0x806044);
            }
        }
        private void dragon() {
            // A coiled mind-dragon, pale gold eye and open ribbed wings; no opaque backdrop.
            for(int i=0;i<32;i++) {
                double a=i/31.0*Math.PI*1.55+.1,b=(i+1)/31.0*Math.PI*1.55+.1;
                double r=10-i*.15;
                line(Math.cos(a)*r,3+Math.sin(a)*r,8,Math.cos(b)*r,3+Math.sin(b)*r,8,.95-i*.021,i%3==0?0x929A9F:0xD2D4CE);
            }
            box(9,-3,8,4,3,2.8,0xD8D9D0);
            box(11.1,-2.5,8,2.5,1.6,2,0xB7C1C2);
            box(9.6,-3.6,9.5,.55,.65,.3,0xE2C969);
            line(8,-4,8,7,-7,8,.4,0xC0C8C9);
            line(9,-1.5,8,10,3,8,.95,0xBFCBD0);
            line(10,-4,7,11,-6,7,.35,0xCDD6D5);
            line(11,-1.5,9,12.5,-2,9,.18,0x72878B);
            for(int side=-1;side<=1;side+=2) for(int i=0;i<3;i++) {
                double xx=side*(12-i*2),yy=-7-i*2+Math.sin(time*.025)*.6;
                line(side*3,3,8,xx,yy,8,.4,0xC5C9C5);
                line(xx,yy,8,side*(12-i*2),3-i,8,.3,0x849996);
                facet(side*3,3,xx,yy,side*(10-i*2),2-i,8,0xBBC5BD);
            }
            ring(0,2,9,3,3,.13,0xE8D699,time*.01);
        }
        private void sun() {
            ring(0,2,8,7.5,7.5,.55,0xFFE4A0,0);
            ring(0,2,8,6.5,6.5,.2,0xC58C37,0);
            for(int i=0;i<12;i++) {
                double a=i*Math.PI/6+time*.002;
                double r=10+(i%2)*2;
                line(Math.cos(a)*8,2+Math.sin(a)*8,8,Math.cos(a)*r,2+Math.sin(a)*r,8,.38,0xF5C35E);
                double b=a+.09;
                line(Math.cos(b)*8,2+Math.sin(b)*8,8,Math.cos(a)*r,2+Math.sin(a)*r,8,.16,0xFFF2BF);
            }
            // Swept feather rays echo the pathway's sunbird without covering the player's face.
            for(int side=-1;side<=1;side+=2) for(int i=0;i<4;i++)
                line(side*6,6-i,8,side*(12+i*.8),1-i*2.2,8,.26,0xD79C44);
        }
        private void twilight() {
            crescent(0,3,8,10,0xA65B36);
            line(0,-8,9,0,14,9,.9,0x877761);
            line(-4,-3,9,4,-3,9,.6,0xC89056);
            line(0,14,9,-1,11,9,.35,0xE0A773);
            for(int i=0;i<5;i++) box((i%2-.5)*.8,1+i*2,9.8,.5,.7,.2,0xC07A40);
            line(-11,7,8,11,7,8,.15,0xDF8B52);
        }
        private void war() {
            for(int side=-1;side<=1;side+=2) {
                line(side*8,-11,8,side*5,13,8,.55,0x6B514A);
                for(int i=0;i<5;i++) {
                    double flutter=Math.sin(time*.07+i*.6)*.45;
                    box(side*(9.1+i*.6),-6+i*.3,8+flutter,1.1,7-i,.7,i%2==0?0xB92E24:0x6E1B1C);
                }
                line(side*8,-11,8,side*7,-8,8,.4,0xE19564);
                line(side*8,-11,8,side*9,-8,8,.4,0xE19564);
                for(int i=0;i<6;i++) {
                    double y=11-((time*.12+i*2)%13);
                    box(side*(9+Math.sin(i+time*.06)),y,7,.45,1.5,.5,i%2==0?0xFFC56E:0xEE5B2D);
                }
            }
        }
        private void abyss() {
            for(int side=-1;side<=1;side+=2) for(int i=0;i<8;i++) {
                double x=side*(8+Math.sin(i*1.8)),y=-7+i*3;
                box(x,y,8,1.6,2.5,1.5,0x785060);
                blade(x,y,x-side*2.7,y-2,.6,9,0x522D35,0x925254);
                line(x,y,8.6,x-side*2,y+1,8.6,.2,i%2==0?0xA44946:0x659398);
            }
            line(-8,-7,8,0,-10,8,.5,0x663B3C);
            line(0,-10,8,8,-7,8,.5,0x663B3C);
        }
        private void night() {
            for(int wisp=0;wisp<5;wisp++) {
                double side=wisp%2==0?1:-1;
                for(int i=0;i<16;i++) {
                    double u=i/15.0,v=(i+1)/15.0;
                    double x=side*(4+u*6+Math.sin(u*5+time*.018+wisp));
                    double xx=side*(4+v*6+Math.sin(v*5+time*.018+wisp));
                    double y=-4+u*18+wisp*.6,yy=-4+v*18+wisp*.6;
                    line(x,y,6+Math.sin(u*4+wisp)*2,xx,yy,6+Math.sin(v*4+wisp)*2,.26,0x687A9B);
                    if(i==6 || i==13) spark(x,y,8,.45,0xD196A4,wisp);
                }
            }
        }
        private void mirrors() {
            for(int side=-1;side<=1;side+=2) for(int i=0;i<3;i++) {
                double x=side*(7+i*2),y=-5+i*5,z=7+i*.7+Math.sin(time*.015+i)*.3;
                blade(x,y+3,x+side*.7,y-4,1.8,z,0x557A91,0xB6D6DE);
                line(x-side*.3,y+2,z+.9,x+side*.7,y-3,z+.9,.12,0xEFF5F0);
                blade(x,y+3,x+Math.sin(time*.04+i),y-1,.55,z+1,0x242330,0x403747);
            }
        }
        private void disorder() {
            for(int i=0;i<3;i++) {
                double y=1+i*4,skew=Math.sin(time*.015+i)*1.3;
                for(int side=-1;side<=1;side+=2) {
                    blade(skew,y+2,side*(10-i),y-3,1.7,7+i*.6,0x645C7C,0xA99566);
                    line(skew,y+2,7.9+i*.6,side*(10-i),y-3,7.9+i*.6,.25,0xBF9B50);
                }
            }
            for(int i=0;i<5;i++) {
                double x=-6+i*3;
                blade(x,-5,x+(i%2==0?-1:1),-10-i%2*2,.7,8,0x8E7136,0xDDC17D);
            }
        }
        private void sacrifice() {
            line(0,-10,8,0,14,8,.7,0x81776F);
            line(-6,6,8,6,6,8,.65,0x81776F);
            for(int i=0;i<28;i++) {
                double y=-8+i*.75,a=i*.65+time*.025;
                box(Math.sin(a)*1.3,y,8+Math.cos(a),.6,.8,.6,i%3==0?0x6C272F:0xA34448);
            }
        }
        private void knowledge() {
            ring(0,1,8,6,3,.45,0xBEA1DA,0);
            line(0,-1,8,0,3,8,.65,0xD6B76C);
            for(int i=0;i<5;i++) {
                double a=i*Math.PI*.4+time*.004,x=Math.cos(a)*10,y=2+Math.sin(a)*10;
                box(x,y,8,3,4.5,.45,0x81749B);
                for(int j=0;j<3;j++) line(x-.8,y-1+j,8.3,x+.5-(j%2)*.5,y-1+j,8.3,.12,0xDBBCEB);
            }
        }
        private void scales() {
            ring(0,2,8,10,10,.2,0xBB9850,0);
            line(0,-6,8,0,12,8,.65,0xE0C87F);
            box(0,-3,8,15,1.1,1.1,0xCBA85A);
            for(int side=-1;side<=1;side+=2) {
                line(side*6,-3,8,side*6,4,8,.14,0xF0DFA6);
                line(side*6,1,8,side*9,5,8,.17,0xC9AA63);
                line(side*6,1,8,side*3,5,8,.17,0xC9AA63);
                line(side*9,5,8,side*3,5,8,.35,0xE0C87F);
                facet(side*9,5,side*3,5,side*6,6.5,8,0xB99A55);
                box(side*6,5.2,8,6,.35,2,0xE0C87F);
            }
        }
        private void gear(double x,double y,double radius,double angle) {
            ring(x,y,8,radius,radius,.75,0xAB8753,angle);
            ring(x,y,9,radius,radius,.65,0xD4B375,angle);
            box(x,y,8.5,2,2,2,0x799AA2);
            for(int i=0;i<12;i++) {
                double a=i*Math.PI/6+angle;
                line(x+Math.cos(a)*(radius-.4),y+Math.sin(a)*(radius-.4),8,
                        x+Math.cos(a)*(radius+1),y+Math.sin(a)*(radius+1),8.5,.65,0xD1B075);
            }
            for(int i=0;i<3;i++) {
                double a=i*Math.PI*2/3+angle;
                line(x,y,8,x+Math.cos(a)*radius,y+Math.sin(a)*radius,8,.2,0x547E91);
            }
        }
        private void gears() { gear(-4,-2,5,time*.015); gear(4,6,5,-time*.015); }
        private void tower() {
            for(int i=0;i<5;i++) {
                double y=12-i*5,w=7-i;
                box(0,y,7+i*.5,w,3.7,3.2,i<3?0xD2CFC2:0x88858D);
                line(-w/2,y-1.3,8.7,w/2,y-1.3,8.7,.12,0xAA925D);
                box(0,y,8.8,.5,.65,.15,0xCDB773);
            }
            for(int side=-1;side<=1;side+=2) for(int i=0;i<3;i++) {
                line(side*5,10-i*5,8,side*(9-i),8-i*5,8,.2,0xD8D6CC);
                line(side*(9-i),8-i*5,8,side*(9-i),4-i*5,8,.2,0xD8D6CC);
                facet(side*5,10-i*5,side*(9-i),8-i*5,side*(9-i),4-i*5,8.5,0xB9B7AB);
            }
        }
        private void storm() {
            for(int i=0;i<48;i++) {
                double a=i*.25+time*.02,b=a+.25,r=4+i*.14;
                line(Math.cos(a)*r,4+Math.sin(a)*r*.7,8,Math.cos(b)*r,4+Math.sin(b)*r*.7,8,.22,i%3==0?0x77C8D8:0x448CAE);
            }
            for(int side=-1;side<=1;side+=2) {
                line(side*6,-9,9,side*3,-2,9,.28,0xBBE3EE);
                line(side*3,-2,9,side*7,-3,9,.28,0xBBE3EE);
                line(side*7,-3,9,side*4,5,9,.28,0xBBE3EE);
            }
        }
    }
}
