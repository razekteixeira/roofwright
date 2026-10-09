"""Renders the Roofwright icon as a 3D voxel diorama with Blender (Cycles).

Every pixel of branding/icon-32.png becomes a bevelled block. Its height depends on what it is: the
night-sky tile stays low, the walls stand up, and the roof rises step by step towards the ridge, so the
logo reads as a little roofed house. Windows and stars glow. Original art only: the input is our own
icon, no game textures.

Run from the project root:
  /Applications/Blender.app/Contents/MacOS/Blender -b -P branding/render_logo3d.py -- branding/logo3d.png
"""

import math
import sys

import bmesh
import bpy

ICON = "branding/icon-32.png"
OUT = sys.argv[sys.argv.index("--") + 1] if "--" in sys.argv else "branding/logo3d.png"

# Icon palette (see make_icon.py) -> kind. Unknown colours fall back to the nearest entry.
PALETTE = {
    (8, 10, 18): "border", (20, 26, 48): "sky", (28, 36, 66): "sky", (226, 232, 255): "star",
    (12, 12, 18): "outline", (214, 96, 50): "roof", (160, 62, 32): "roof", (246, 150, 92): "roof",
    (234, 222, 196): "wall", (198, 182, 152): "wall", (255, 180, 50): "window", (196, 112, 20): "window",
    (96, 62, 38): "door", (66, 116, 58): "grass", (48, 88, 44): "grass",
}
BASE = {"border": 0.5, "sky": 0.35, "star": 0.9, "outline": 1.2, "wall": 2.0, "window": 1.7,
        "door": 1.5, "grass": 0.9, "roof": 2.4}
ROOF_TOP_ROW, ROOF_BOTTOM_ROW = 7, 18


def nearest_kind(rgb):
    return min(PALETTE.items(), key=lambda item: sum((a - b) ** 2 for a, b in zip(item[0], rgb)))[1]


def srgb_to_linear(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 256
    scene.cycles.use_denoising = True
    try:
        prefs = bpy.context.preferences.addons["cycles"].preferences
        prefs.compute_device_type = "METAL"
        prefs.get_devices()
        for device in prefs.devices:
            device.use = True
        scene.cycles.device = "GPU"
    except Exception as error:  # CPU fallback keeps the script portable
        print("GPU unavailable, using CPU:", error)
    scene.render.film_transparent = True
    scene.render.resolution_x = 1600
    scene.render.resolution_y = 1600
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.view_settings.view_transform = "AgX"
    scene.view_settings.look = "AgX - Punchy"
    world = bpy.data.worlds.new("World")
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs[0].default_value = (0.012, 0.016, 0.03, 1)
    world.node_tree.nodes["Background"].inputs[1].default_value = 1.0
    scene.world = world
    return scene


def material(name, metallic, roughness, emission=0.0):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes["Principled BSDF"]
    attr = nodes.new("ShaderNodeAttribute")
    attr.attribute_name = "Col"
    mat.node_tree.links.new(attr.outputs["Color"], bsdf.inputs["Base Color"])
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    if emission:
        mat.node_tree.links.new(attr.outputs["Color"], bsdf.inputs["Emission Color"])
        bsdf.inputs["Emission Strength"].default_value = emission
    return mat


def height(kind, x, y, kinds):
    h = BASE[kind]
    if kind == "roof":
        # Each two-row step of the roof sits a little higher than the one below it.
        h += 0.35 * ((ROOF_BOTTOM_ROW - y) // 2)
    if kind == "outline":
        # Outlines rise with what they outline, so the house reads as one solid.
        neighbours = [kinds.get((x + dx, y + dy)) for dx in (-1, 0, 1) for dy in (-1, 0, 1)]
        tall = [height(k, x, y, kinds) for k in neighbours if k in ("roof", "wall", "window", "door")]
        if tall:
            h = max(tall) - 0.1
    return h


def build_voxels():
    image = bpy.data.images.load(bpy.path.abspath("//" + ICON) if bpy.data.filepath else ICON)
    w, h = image.size
    px = list(image.pixels)
    cells = []
    for y in range(h):
        for x in range(w):
            i = ((h - 1 - y) * w + x) * 4  # Blender stores rows bottom-up
            r, g, b, a = px[i:i + 4]
            if a < 0.5:
                continue
            rgb8 = tuple(round(c * 255) for c in (r, g, b))
            cells.append((x, y, nearest_kind(rgb8), (r, g, b)))
    kinds = {(x, y): kind for x, y, kind, _ in cells}
    groups = {"matte": material("Matte", 0.0, 0.6), "roof": material("Roof", 0.0, 0.42),
              "glow": material("Glow", 0.0, 0.35, emission=4.0)}
    group_of = {"roof": "roof", "window": "glow", "star": "glow"}
    meshes = {name: (bmesh.new(), []) for name in groups}
    for x, y, kind, rgb in cells:
        bm, colors = meshes[group_of.get(kind, "matte")]
        top = height(kind, x, y, kinds)
        cx, cy = x + 0.5, -(y + 0.5)
        verts = [bm.verts.new((cx + sx * 0.5, cy + sy * 0.5, z))
                 for z in (0.0, top) for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
        for f in ((0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7)):
            bm.faces.new([verts[k] for k in f])
            colors.append(tuple(srgb_to_linear(c) for c in rgb) + (1.0,))
    pivot = bpy.data.objects.new("Pivot", None)
    bpy.context.collection.objects.link(pivot)
    for name, (bm, colors) in meshes.items():
        mesh = bpy.data.meshes.new(name)
        bm.to_mesh(mesh)
        bm.free()
        attribute = mesh.color_attributes.new(name="Col", type="FLOAT_COLOR", domain="CORNER")
        for poly in mesh.polygons:
            for loop_index in poly.loop_indices:
                attribute.data[loop_index].color = colors[poly.index]
        obj = bpy.data.objects.new(name, mesh)
        bpy.context.collection.objects.link(obj)
        obj.data.materials.append(groups[name])
        bevel = obj.modifiers.new("Bevel", "BEVEL")
        bevel.width = 0.07
        bevel.segments = 3
        bevel.limit_method = "NONE"
        obj.parent = pivot
        obj.location = (-16, 16, 0)
    # Stand the tile up like a badge, slightly turned, pivoting on its centre.
    pivot.rotation_euler = (math.radians(72), 0, math.radians(-18))
    pivot.location = (16, -16, 15)


def point_at(obj, target):
    direction = [t - o for t, o in zip(target, obj.location)]
    length = math.sqrt(sum(d * d for d in direction))
    dx, dy, dz = (d / length for d in direction)
    obj.rotation_euler = (math.atan2(math.hypot(dx, dy), -dz), 0, math.atan2(dy, dx) - math.pi / 2)


def area_light(name, location, energy, color, size):
    data = bpy.data.lights.new(name, "AREA")
    data.energy = energy
    data.color = color
    data.size = size
    obj = bpy.data.objects.new(name, data)
    obj.location = location
    bpy.context.collection.objects.link(obj)
    point_at(obj, (16, -16, 15))


def camera():
    data = bpy.data.cameras.new("Camera")
    data.lens = 62
    cam = bpy.data.objects.new("Camera", data)
    cam.location = (16 + 22, -16 - 92, 22)
    bpy.context.collection.objects.link(cam)
    point_at(cam, (16, -16, 15))
    bpy.context.scene.camera = cam


scene = reset_scene()
build_voxels()
area_light("Key", (-25, -60, 45), 32000, (1.0, 0.94, 0.86), 30)
area_light("Top", (16, -30, 70), 9000, (1.0, 1.0, 1.0), 30)
area_light("Fill", (60, -45, 15), 4000, (0.6, 0.72, 1.0), 20)
area_light("Rim", (16, 25, 40), 9000, (1.0, 0.7, 0.4), 20)
camera()
scene.render.filepath = OUT
bpy.ops.render.render(write_still=True)
print("rendered", OUT)
