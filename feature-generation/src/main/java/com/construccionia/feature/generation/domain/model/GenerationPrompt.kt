package com.construccionia.feature.generation.domain.model

/**
 * Prompt de generación de imagen enviado al modelo Gemini.
 */
data class GenerationPrompt(
    val text: String,
    val style: PromptStyle = PromptStyle.FOTO_REALISTA,
    val aspectRatio: AspectRatio = AspectRatio.SQUARE_1_1
) {
    /**
     * Construye el prompt completo combinando texto, estilo y proporción.
     */
    fun toFullPrompt(): String {
        val styleInstruction = when (style) {
            PromptStyle.FOTO_REALISTA -> "Estilo: foto realista, alta definición, iluminación natural"
            PromptStyle.ESQUEMA_TECNICO -> "Estilo: esquema técnico, líneas claras, diagrama arquitectónico, blanco y negro"
            PromptStyle.RENDER_3D -> "Estilo: render 3D fotorrealista, texturas detalladas, sombras suaves"
            PromptStyle.BOCETO -> "Estilo: boceto a mano alzada, trazos suaves, aspecto inacabado"
        }
        val ratioInstruction = when (aspectRatio) {
            AspectRatio.SQUARE_1_1 -> "Proporción: 1:1 (cuadrado)"
            AspectRatio.LANDSCAPE_4_3 -> "Proporción: 4:3 (apaisado)"
            AspectRatio.LANDSCAPE_16_9 -> "Proporción: 16:9 (pantalla ancha)"
            AspectRatio.PORTRAIT_3_4 -> "Proporción: 3:4 (retrato)"
            AspectRatio.PORTRAIT_9_16 -> "Proporción: 9:16 (vertical)"
        }
        return "$text\n\n$styleInstruction\n$ratioInstruction"
    }
}
