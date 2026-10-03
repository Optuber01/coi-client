# Mythical Creature Forms System

This document outlines the architecture, network protocol, rendering pipeline, and existing implementations of the Mythical Creature Forms system in the COI Client mod. Use this guide to understand how forms are registered, processed, and rendered, and how to improve their aesthetics.

---

## 1. Project Overview & Architecture

The Mythical Creature Forms system allows players to transform into mythical beings based on their pathway (as described in `mythical_creatures.json`). The server controls transformations, and the client handles all procedural rendering.

### Key Classes

* **[`MythicalCreatureForm.java`](../src/main/java/dev/ua/ikeepcalm/coi/domain/form/MythicalCreatureForm.java)**: The
  base interface for all forms. Contains default methods `drawBox` and `addVertex` for drawing 3D geometry.
* **[`MythicalFormManager.java`](../src/main/java/dev/ua/ikeepcalm/coi/domain/form/MythicalFormManager.java)**: The
  registry that manages active transformations per player. Handles key normalization (spaces, underscores, casing) and
  dispatches rendering to the active form.
* **[`LivingEntityRendererMixin.java`](../src/main/java/dev/ua/ikeepcalm/coi/mixin/LivingEntityRendererMixin.java)**:
  Intercepts `LivingEntityRenderer`'s `submit` method. If the entity is a player with an active form, it cancels vanilla
  rendering and submits custom geometry.
* **[`EffectDebugScreen.java`](../src/main/java/dev/ua/ikeepcalm/coi/screen/debug/EffectDebugScreen.java)**: Developer
  debug screen (opened via F8 in dev environment) that contains a dynamic cycling button to test all 20 registered forms
  on the local player.

---

## 2. Rendering Pipeline (Minecraft 1.21.2+)

In Minecraft 1.21.2+, the traditional `render(...)` model in entity renderers is replaced with a deferred, state-based system using `SubmitNodeCollector`.

### Interception & Matrix Rotations

The custom rendering starts in [
`LivingEntityRendererMixin.java`](../src/main/java/dev/ua/ikeepcalm/coi/mixin/LivingEntityRendererMixin.java):
1.  **Gaze Alignment**: Before rendering the custom form, the mixin rotates the `PoseStack` around the Y-axis using `state.bodyRot` (which holds the player's head/camera look direction):
    ```java
    poseStack.pushPose();
    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - state.bodyRot));
    ```
2.  **Custom Geometry Submission**: The geometry is submitted using a white texture `coi-client:textures/entity/white.png` (translucent color overlays are applied per vertex):
    ```java
    collector.order(0).submitCustomGeometry(
        poseStack,
        RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png")),
        (pose, consumer) -> MythicalFormManager.renderFormSubmit(form, avatarState, pose, consumer)
    );
    ```
3.  **Nameplate Preservation**: The mixin invokes `callSubmitNameDisplay` to ensure player name tags remain visible above the custom form, then cancels vanilla player model rendering with `ci.cancel()`.

---

## 3. Creating & Modifying Forms

All forms are located in the package [
`dev.ua.ikeepcalm.coi.domain.form.pathway`](../src/main/java/dev/ua/ikeepcalm/coi/domain/form/pathway) and implement
`MythicalCreatureForm`.

### Draw Helpers
The `MythicalCreatureForm` interface provides:
*   `drawBox(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha, light)`: Draws a 3D box at local coordinates with custom colors (0.0f - 1.0f) and translucency.
*   `addVertex(pose, consumer, x, y, z, r, g, b, alpha, u, v, nx, ny, nz, light)`: Directly feeds vertices to the buffer.

### Boilerplate Example
```java
package dev.ua.ikeepcalm.coi.domain.form.pathway;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.domain.form.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class MyNewForm implements MythicalCreatureForm {
    @Override
    public String getPathwayName() {
        return "My New Form";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;
        // Render shapes using drawBox...
    }
}
```

---

## 4. Guide for Improving Aesthetics

When optimizing the visual appearance of these forms, consider the following parameters:
1.  **Colors**: Keep them vibrant and aligned with their pathway descriptors (e.g., `Fool` = purple/indigo, `Tyrant` = lightning cyan, `Sun` = blazing gold, `Death` = green gas/bone white).
2.  **Opacity**: Adjust the alpha channels (e.g. `0.2f` for glassmorphic elements, `0.95f` for dense core shadows) to make shapes feel dimensional and ethereal.
3.  **Procedural Math**: Add more segments or sub-motions. You can introduce multiple sine/cosine frequencies in the `time` parameter inside individual form classes to avoid repetitive patterns.
4.  **Density**: Adjust loops (like `maggotCount`, `particleCount`, `gear teeth`) for fuller volumes, but keep CPU calculations cheap to preserve client-side frames.

## Full forms, partial forms and the carrier transform

S→C `coi-client:mythical` (`MythicalFormPayload`, `targetUuid` + `pathway:<unused>:start|stop`) marks
a player's UUID as transformed into a pathway's form. Two kinds:

- **Full forms** — the vanilla player render is cancelled outright (`LivingEntityRendererMixin` at HEAD)
  and replaced with procedural geometry drawn from `FormPrimitives`. 19 of the 20 pathways.
- **Partial forms** — a baked Blockbench model stands in for the *lower body* while the player's own
  head/torso/arms keep rendering. Currently Visionary only (`FormModelLayers.VISIONARY_LOWER_SPEC`).

Partial forms are assembled from four pieces that all have to agree:

| Piece                                 | Job                                                                               |
|---------------------------------------|-----------------------------------------------------------------------------------|
| `PlayerModelMixin` (`setupAnim` TAIL) | hides leg parts; must run in `setupAnim`, since submission is deferred            |
| `HumanoidArmorLayerMixin`             | hides leggings/boots, which draw from their own model set                         |
| `LivingEntityRendererMixin`           | `hipRaise` push (world space, at HEAD) + **carrier transform** push (model space) |
| `PartialFormLayer`                    | draws the baked model, undoing the carrier transform it inherits                  |

**The carrier transform** is what makes the halves read as one body. The rig's torso bone (its
"carrier") both rotates and translates during the walk cycle, around a pivot that is over a block
away from the player's waist. `FormModel#carrierDelta` hands out that bone's full rigid motion,
`PartialForms#carrierTransform` converts it into player space, and the renderer mixin pushes it onto
the pose stack just before the model is submitted — so the player *and* every layer above it (armor,
held items, cape, appearance traits) ride the torso exactly. Copying the rotation angle alone is not
enough and looks like shearing: same tilt, wrong pivot, no translation.

Placement knobs live in `FormModelLayers`; read the comment there before touching one. `hipRaise` and
the carrier push sit in different coordinate spaces on purpose — see `LivingEntityRendererMixin`.

`domain/form/model/VisionaryLowerModel` and `VisionaryLowerAnimations` are **Blockbench exports**. They are
generated files: change the rig in the modelling tool and re-export, never hand-edit the Java. A
hand edit is invisible until the next export silently reverts it.

**Dev testing** (no server needed): F8 → *Form: None (Click to cycle)* applies a form to yourself.
