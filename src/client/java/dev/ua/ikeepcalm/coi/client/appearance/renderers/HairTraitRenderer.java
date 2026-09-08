package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Textured scalp clumps and continuous tapered locks, with head and torso attachments kept separate. */
public final class HairTraitRenderer implements AppearanceTraitRenderer {
    public enum Style {SHORT, LONG}
    private static final TraitGeometry G = TraitGeometry.INSTANCE;
    private static final RenderType MATERIAL = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/hair_strands.png"));
    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint base, highlight;

    public HairTraitRenderer(String traitId, Style style, float red, float green, float blue) {
        this.traitId = traitId;
        this.style = style;
        float lift = Math.max(red, Math.max(green, blue)) < .06f ? .09f : 0;
        base = new TraitGeometry.Tint(red + lift, green + lift, blue + lift, 1);
        highlight = new TraitGeometry.Tint(Math.min(1, base.r() * 1.35f + .025f),
                Math.min(1, base.g() * 1.35f + .025f), Math.min(1, base.b() * 1.35f + .025f), 1);
    }

    @Override public String traitId() { return traitId; }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        var settings = AppearanceConfig.get();
        int light = state.lightCoords;
        float stride = Math.clamp(state.walkAnimationSpeed, 0, 1);
        stack.pushPose();
        model.head.translateAndRotate(stack);
        stack.translate(0, settings.hairYOffsetPixels / 16, 0);
        collector.order(1).submitCustomGeometry(stack, MATERIAL, (pose, consumer) -> head(pose, consumer, light));
        stack.popPose();
        if (style == Style.LONG) {
            stack.pushPose();
            model.body.translateAndRotate(stack);
            stack.translate(0, settings.hairYOffsetPixels / 16, 0);
            stack.scale(1, settings.hairLength, 1);
            collector.order(1).submitCustomGeometry(stack, MATERIAL, (pose, consumer) -> locks(pose, consumer, light, stride));
            stack.popPose();
        }
    }

    private void head(PoseStack.Pose p, VertexConsumer c, int light) {
        // Broad overlapping clumps break up the crown without increasing the head silhouette substantially.
        box(p,c,-4.22f,-8.35f,-4.18f,-1.35f,-6.55f,4.2f,base,light);
        box(p,c,-1.4f,-8.62f,-4.23f,1.45f,-6.55f,4.25f,base,light);
        box(p,c,1.4f,-8.43f,-4.18f,4.22f,-6.55f,4.2f,base,light);
        box(p,c,-4.22f,-6.75f,3.72f,4.22f,style==Style.SHORT?-.45f:1,4.35f,base,light);
        box(p,c,-4.28f,-6.7f,-3.5f,-3.7f,-.3f,4.05f,base,light);
        box(p,c,3.7f,-6.7f,-3.5f,4.28f,-.3f,4.05f,base,light);
        box(p,c,-4.05f,-6.75f,-4.34f,-1.35f,-4.45f,-4.05f,base,light);
        box(p,c,-1.45f,-6.72f,-4.35f,.15f,-4.05f,-4.04f,highlight,light);
        box(p,c,.08f,-6.72f,-4.34f,2.05f,-4.7f,-4.04f,base,light);
        box(p,c,1.95f,-6.72f,-4.33f,4.04f,-5.25f,-4.04f,base,light);
        for(int i=0;i<9;i++) {
            float x=-4.1f+i*.92f;
            if(style==Style.SHORT) box(p,c,x,-.7f,3.72f,x+.86f,.1f+(i%3)*.3f,4.35f,base,light);
        }
        for(int side=-1;side<=1;side+=2) for(int i=0;i<7;i++) {
            float x=side<0?-4.28f:3.7f,z=-3.4f+i*1.05f;
            box(p,c,x,-.5f,z,x+.58f,.05f+(i%3)*.25f,z+.95f,base,light);
        }
    }

    private void locks(PoseStack.Pose p, VertexConsumer c, int light, float stride) {
        for(int lock=0;lock<9;lock++) {
            float center=-3.5f+lock*.875f;
            float end=14.4f+(4-Math.abs(lock-4))*.65f+(float)Math.sin(lock*2.1)*.35f;
            float px=center,py=-1.3f,pz=3.8f,pw=.48f,pd=.65f;
            var tint=lock%3==1?highlight:base;
            for(int section=1;section<=8;section++) {
                float t=section/8f;
                float x=center+(float)Math.sin(lock*.8+t*2)*.18f*t;
                float y=-1.3f+(end+1.3f)*t,z=3.8f+t*t*2.3f+stride*8.5f*t;
                float w=section==8?.09f:.48f*(1-.45f*t),d=.65f*(1-.45f*t);
                // Four continuous surfaces share UVs across the full drape, not per tiny strand.
                quad(p,c,px-pw,py,pz+pd,px+pw,py,pz+pd,x+w,y,z+d,x-w,y,z+d,tint,light,true);
                quad(p,c,px+pw,py,pz,px-pw,py,pz,x-w,y,z,x+w,y,z,tint,light,true);
                quad(p,c,px-pw,py,pz,px-pw,py,pz+pd,x-w,y,z+d,x-w,y,z,tint,light,true);
                quad(p,c,px+pw,py,pz+pd,px+pw,py,pz,x+w,y,z,x+w,y,z+d,tint,light,true);
                px=x;py=y;pz=z;pw=w;pd=d;
            }
            quad(p,c,px-pw,py,pz,px-pw,py,pz+pd,px+pw,py,pz+pd,px+pw,py,pz,base,light,true);
        }
    }

    private static void box(PoseStack.Pose p,VertexConsumer c,float x0,float y0,float z0,
                            float x1,float y1,float z1,TraitGeometry.Tint t,int light) {
        quad(p,c,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,t,light,false);
        quad(p,c,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0,t,light,false);
        quad(p,c,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,t,light,false);
        quad(p,c,x0,y0,z0,x0,y0,z1,x1,y0,z1,x1,y0,z0,t,light,false);
        quad(p,c,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1,t,light,false);
        quad(p,c,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,t,light,false);
    }

    private static void quad(PoseStack.Pose p,VertexConsumer c,
                             float ax,float ay,float az,float bx,float by,float bz,
                             float cx,float cy,float cz,float dx,float dy,float dz,
                             TraitGeometry.Tint t,int light,boolean drape) {
        float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length<.0001f)return;
        nx/=length;ny/=length;nz/=length;
        vertex(p,c,ax,ay,az,nx,ny,nz,t,light,drape);
        vertex(p,c,bx,by,bz,nx,ny,nz,t,light,drape);
        vertex(p,c,cx,cy,cz,nx,ny,nz,t,light,drape);
        vertex(p,c,dx,dy,dz,nx,ny,nz,t,light,drape);
    }

    private static void vertex(PoseStack.Pose p,VertexConsumer c,float x,float y,float z,
                               float nx,float ny,float nz,TraitGeometry.Tint t,int light,boolean drape) {
        float u=drape?(x+4.3f)/8.6f:(Math.abs(nx)>.5f?z+4.5f:x+4.5f)/9;
        float v=drape?(y+2)/20:(Math.abs(ny)>.5f?(z+4.5f)/9:(y+9)/10);
        G.addVertex(p,c,x/16,y/16,z/16,t.r(),t.g(),t.b(),t.a(),u,v,nx,ny,nz,light);
    }
}
