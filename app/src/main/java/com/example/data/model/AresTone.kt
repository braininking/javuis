package com.example.data.model

enum class AresTone(
  val displayName: String,
  val iconName: String,
  val description: String,
  val systemPromptModifier: String
) {
  NATURAL(
    displayName = "Natural",
    iconName = "chat",
    description = "Estilo do dia a dia, fluido e amigável",
    systemPromptModifier = "Responda de forma natural, amigável, humana e fluida, como uma conversa espontânea de mensageiro."
  ),
  ENGRAÇADO(
    displayName = "Engraçado",
    iconName = "mood",
    description = "Humor descontraído, espirituoso e divertido",
    systemPromptModifier = "Responda com humor inteligente, espirituoso, descontraído e carismático, adequado para uma conversa de WhatsApp/Instagram."
  ),
  ROMANTICO(
    displayName = "Romântico",
    iconName = "favorite",
    description = "Carinhoso, charmoso e atencioso",
    systemPromptModifier = "Responda com carinho, charme, afeto e delicadeza romântica, sem ser exagerado."
  ),
  EDUCADO(
    displayName = "Educado",
    iconName = "handshake",
    description = "Gentil, cortês e respeitoso",
    systemPromptModifier = "Responda com máxima gentileza, cortesia, empatia e respeito aos detalhes."
  ),
  DIRETO(
    displayName = "Direto",
    iconName = "bolt",
    description = "Objetivo, conciso e sem rodeios",
    systemPromptModifier = "Responda de forma curta, concisa, direta ao ponto e sem enrolação."
  ),
  PROFISSIONAL(
    displayName = "Profissional",
    iconName = "work",
    description = "Formal, corporativo e elegante",
    systemPromptModifier = "Responda de forma polida, profissional, elegante e assertiva, ideal para trabalho ou comunicação formal."
  )
}
