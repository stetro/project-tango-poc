Shader "Tango/Ball" {
	
	SubShader {
	    Tags { 
	    	"Queue" = "Background" 
	    }
	    Pass {
			Blend Zero One
			Lighting On
			ZWrite On
			Material {
				Diffuse (0,0,0,0)
			}
		}
	}
}