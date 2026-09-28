# Custom Filament materials

Source `.mat` files for the 3D solar system. They are compiled to `.filamat` binaries in
`app/src/main/assets/materials/` with Filament's `matc` **of the same version as the Filament
runtime** (1.56.0, pulled in by SceneView 2.3.0). A version mismatch fails at runtime.

Rebuild after editing (matc from https://github.com/google/filament/releases/tag/v1.56.0, linux tgz):

    matc -p mobile -a opengl -o app/src/main/assets/materials/space_unlit_textured.filamat materials/space_unlit_textured.mat
    matc -p mobile -a opengl -o app/src/main/assets/materials/space_unlit_color.filamat materials/space_unlit_color.mat
