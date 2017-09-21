# distortion

An Android app that lets you distort the camera preview with your finger. 

Only been tested on API level 26.

# How it works

Using the Camera2 API, the camera preview is sent to a SurfaceTexture that is already bound to a given OpenGL texture.

This OpenGL texture is then mapped onto a grid mesh.

The locations of the vertices of the grid mesh is what can be manipulated using touch input.

# Next steps
* Two finger input
* Image capture/recording
* GUI controls for density of the vertices
* Put vertex translation calculations onto GPU also
