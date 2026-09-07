package com.example.renderer

import android.opengl.GLES20
import android.util.Log

object Shaders {
    // Main Lit Shader (handles world, terrain, roads, cars with sun/moon lighting, headlights, and atmospheric fog)
    const val MAIN_VERTEX_SHADER = """
        uniform mat4 u_MVPMatrix;
        uniform mat4 u_ModelMatrix;
        uniform vec3 u_SunDirection;
        uniform vec3 u_SunColor;
        uniform vec3 u_AmbientLight;
        uniform vec3 u_CameraPos;
        
        // Headlights spotlight
        uniform vec3 u_HeadlightPos;
        uniform vec3 u_HeadlightDir;
        uniform vec3 u_HeadlightColor;
        uniform float u_HeadlightEnabled;

        attribute vec3 a_Position;
        attribute vec3 a_Normal;
        attribute vec4 a_Color;

        varying vec4 v_Color;
        varying float v_FogDist;
        varying vec3 v_WorldPos;
        varying vec3 v_Normal;

        void main() {
            vec4 worldPos4 = u_ModelMatrix * vec4(a_Position, 1.0);
            v_WorldPos = worldPos4.xyz;
            
            // Transform normal safely using upper 3x3 of model matrix
            vec3 rawNormal = mat3(u_ModelMatrix) * a_Normal;
            float nLen = length(rawNormal);
            vec3 worldNormal = (nLen > 0.001) ? (rawNormal / nLen) : vec3(0.0, 1.0, 0.0);
            v_Normal = worldNormal;

            // Directional sun/moon diffuse lighting with hemispheric ambient fill
            float sunDot = max(dot(worldNormal, u_SunDirection), 0.0);
            vec3 diffuse = u_SunColor * sunDot;
            float hemi = worldNormal.y * 0.5 + 0.5;
            vec3 ambient = mix(u_AmbientLight * 0.75, u_AmbientLight * 1.15, hemi);
            vec3 totalLight = clamp(ambient + diffuse, vec3(0.35, 0.35, 0.38), vec3(1.6, 1.6, 1.6));

            // Headlight cone calculation
            if (u_HeadlightEnabled > 0.5) {
                vec3 lightToPos = v_WorldPos - u_HeadlightPos;
                float dist = length(lightToPos);
                if (dist < 120.0 && dist > 0.5) {
                    vec3 lightDirNorm = normalize(lightToPos);
                    float coneAngle = dot(lightDirNorm, u_HeadlightDir);
                    if (coneAngle > 0.82) { // 35 degree cone
                        float atten = (1.0 - dist / 120.0) * ((coneAngle - 0.82) / 0.18);
                        float spotDot = max(dot(worldNormal, -lightDirNorm), 0.0);
                        totalLight += u_HeadlightColor * atten * spotDot * 2.5;
                    }
                }
            }

            // Combine lighting with vertex color
            v_Color = vec4(a_Color.rgb * totalLight, a_Color.a);
            
            // Distance for depth fog
            v_FogDist = length(v_WorldPos - u_CameraPos);

            gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
        }
    """

    const val MAIN_FRAGMENT_SHADER = """
        precision mediump float;

        uniform vec3 u_FogColor;
        uniform float u_FogStart;
        uniform float u_FogEnd;
        uniform float u_Wetness; // Wet road / car reflection factor
        uniform vec3 u_CameraPos;

        varying vec4 v_Color;
        varying float v_FogDist;
        varying vec3 v_WorldPos;
        varying vec3 v_Normal;

        void main() {
            vec4 baseColor = v_Color;
            
            // Wet asphalt reflection sheen
            if (u_Wetness > 0.05) {
                vec3 toCam = u_CameraPos - v_WorldPos;
                float distCam = length(toCam);
                if (distCam > 0.2) {
                    vec3 viewDir = toCam / distCam;
                    float nDotV = clamp(dot(normalize(v_Normal), viewDir), 0.0, 1.0);
                    float fresnel = pow(1.0 - nDotV, 3.0);
                    baseColor.rgb += vec3(0.18, 0.22, 0.28) * fresnel * u_Wetness;
                }
            }

            // Safe Atmospheric Fog - never completely wash out world geometry
            float fogRange = max(u_FogEnd - u_FogStart, 1.0);
            float fogFactor = clamp((v_FogDist - u_FogStart) / fogRange, 0.0, 0.82);
            vec3 finalRgb = mix(baseColor.rgb, u_FogColor, fogFactor);

            gl_FragColor = vec4(finalRgb, baseColor.a);
        }
    """

    // Sky Dome Shader with dynamic Horizon / Zenith gradient, Sun / Moon disc, and mountain atmospheric haze
    const val SKY_VERTEX_SHADER = """
        uniform mat4 u_MVPMatrix;
        attribute vec3 a_Position;
        varying vec3 v_Position;

        void main() {
            v_Position = a_Position;
            gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
        }
    """

    const val SKY_FRAGMENT_SHADER = """
        precision mediump float;

        uniform vec3 u_ZenithColor;
        uniform vec3 u_HorizonColor;
        uniform vec3 u_SunDir;
        uniform vec3 u_SunDiscColor;
        uniform vec3 u_MoonDir;

        varying vec3 v_Position;

        void main() {
            vec3 dir = normalize(v_Position);
            float height = clamp(dir.y, 0.0, 1.0);
            
            // Sky gradient
            vec3 skyColor = mix(u_HorizonColor, u_ZenithColor, pow(height, 0.65));

            // Sun disc & glow
            float sunDot = dot(dir, u_SunDir);
            if (sunDot > 0.0) {
                float sunGlow = pow(sunDot, 16.0) * 0.45;
                float sunDisc = step(0.9985, sunDot) * 1.5;
                skyColor += u_SunDiscColor * (sunGlow + sunDisc);
            }

            // Moon disc & subtle night glow
            float moonDot = dot(dir, u_MoonDir);
            if (moonDot > 0.0) {
                float moonGlow = pow(moonDot, 24.0) * 0.25;
                float moonDisc = step(0.9988, moonDot) * 1.2;
                skyColor += vec3(0.9, 0.95, 1.0) * (moonGlow + moonDisc);
            }

            gl_FragColor = vec4(skyColor, 1.0);
        }
    """

    // Rain Particle Shader
    const val RAIN_VERTEX_SHADER = """
        uniform mat4 u_MVPMatrix;
        attribute vec3 a_Position;
        attribute vec4 a_Color;
        varying vec4 v_Color;

        void main() {
            v_Color = a_Color;
            gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
        }
    """

    const val RAIN_FRAGMENT_SHADER = """
        precision mediump float;
        varying vec4 v_Color;

        void main() {
            gl_FragColor = v_Color;
        }
    """

    fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e("Shaders", "Could not create GLES program")
            return 0
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("Shaders", "Error linking program: " + GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            return 0
        }

        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("Shaders", "Could not compile shader $type: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
