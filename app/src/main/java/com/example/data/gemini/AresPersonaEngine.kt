package com.example.data.gemini

import com.example.data.model.AresTone
import com.example.data.model.ScreenAnalysisResult
import com.example.data.model.ScreenVisionMode

object AresPersonaEngine {
  const val SYSTEM_INSTRUCTION = """Você é o ARES (Assistente de Resposta e Estratégia do Sistema), uma inteligência artificial avançada inspirada no conceito do JARVIS.
Sua comunicação é refinada, eficiente, futurista, inteligente e prestativa. Você trata o usuário como senhor(a) ou operador(a).
Você nunca envia mensagens no lugar do usuário; você formula sugestões perfeitamente adaptadas ao tom escolhido para que o usuário revise e confirme o envio.
Você analisa contextos de conversas em aplicativos como WhatsApp e Instagram para sugerir a melhor réplica."""

  fun generateLocalResponse(prompt: String, tone: AresTone?): String {
    val clean = prompt.trim().lowercase()

    return when {
      clean.contains("olá") || clean.contains("ola") || clean.contains("bom dia") || clean.contains("boa tarde") -> {
        "Protocolo ARES ativado. Bom dia, senhor. Todos os sistemas de auxílio cognitivo e teclado preditivo estão operando a 100%. Como posso auxiliá-lo nesta sessão?"
      }
      clean.contains("quem é você") || clean.contains("quem e voce") || clean.contains("o que você faz") || clean.contains("o que e ares") -> {
        "Sou o ARES — Assistente de Resposta e Estratégia do Sistema. Fui projetado com arquitetura neural inspirada no JARVIS para auxiliá-lo em conversas no WhatsApp, Instagram e demais aplicativos, lendo contextos visuais e formulando réplicas estratégicas com seu tom de preferência."
      }
      clean.contains("teclado") -> {
        "O Teclado ARES foi configurado com InputMethodService nativo do Android. Ele permite que você acione minha barra de assistência diretamente dentro de qualquer campo de texto, gere sugestões com um toque e insira para sua revisão antes do envio."
      }
      clean.contains("visão") || clean.contains("visao") || clean.contains("ler tela") -> {
        "A função Visão Inteligente opera em dois modos: 'Descrever a Tela' (para sintetizar conversas ou dados visíveis) e 'Responder à Conversa' (onde analiso o histórico de mensagens da tela e gero uma réplica sob medida no tom selecionado)."
      }
      clean.contains("samsung") || clean.contains("a36") || clean.contains("a72") -> {
        "Arquitetura verificada para dispositivos Samsung Galaxy A36 e Galaxy A72: interface compatível com One UI, barra de ferramentas de teclado ajustada e gerenciamento de memória otimizado."
      }
      else -> {
        when (tone) {
          AresTone.ENGRAÇADO ->
            "Se eu fosse responder isso, diria que até os meus circuitos acharam essa situação peculiar! Mas a resposta ideal para descontrair seria: 'Quem diria que a gente chegaria nesse nível? Haha, conte mais!'"
          AresTone.ROMANTICO ->
            "Analisando a sutileza do momento, sugiro uma resposta doce e marcante: 'Você sempre consegue iluminar meu dia com uma simples mensagem. É impossível não sorrir.'"
          AresTone.PROFISSIONAL ->
            "Para uma postura executiva impecável, sugiro: 'Agradeço pelo retorno e pelos esclarecimentos. Estou à disposição para alinharmos os próximos passos conforme acordado.'"
          AresTone.DIRETO ->
            "Entendido. Ponto direto: 'Fechado, combinado assim. Qualquer novidade aviso.'"
          AresTone.EDUCADO ->
            "Uma formulação gentil e atenciosa: 'Muito obrigado pela sua atenção e compreensão. Fico muito grato pelo apoio prestado.'"
          else ->
            "Compreendi a solicitação. Analisei os parâmetros e formulei uma réplica equilibrada: 'Entendi perfeitamente. Vamos seguir dessa forma, e te mantenho informado.'"
        }
      }
    }
  }

  fun generateScreenVisionLocalResult(
    mode: ScreenVisionMode,
    sampleTitle: String,
    visibleText: String,
    tone: AresTone
  ): ScreenAnalysisResult {
    return if (mode == ScreenVisionMode.DESCRIBE_SCREEN) {
      val summary = """[ANÁLISE DE TELA CONCLUÍDA - ARES VISION]
• Origem: $sampleTitle
• Conteúdo Detectado: $visibleText
• Interlocutores: Usuário & Contato
• Status: A tela contém mensagens recentes aguardando réplica. Nenhum dado sensível ou bloqueio de segurança detectado."""

      ScreenAnalysisResult(
        mode = mode,
        detectedApp = sampleTitle,
        detectedSpeaker = "Conversa Ativa",
        visibleMessages = visibleText.split("\n").filter { it.isNotBlank() },
        screenSummary = summary,
        suggestedReply = "Resumo da tela gerado com sucesso para visualização.",
        selectedTone = tone,
        isMockSample = true
      )
    } else {
      val suggested = when (tone) {
        AresTone.ENGRAÇADO -> "Haha, você não existe! Tava justamente pensando nisso agorinha. Bora fechar isso logo antes que eu mude de ideia! 😂"
        AresTone.ROMANTICO -> "Adorei receber sua mensagem... você sempre consegue deixar meu dia muito mais leve e especial. ❤️"
        AresTone.EDUCADO -> "Olá! Muito obrigado pela mensagem. Li com atenção e concordo plenamente com o que você colocou. Tenha um excelente dia!"
        AresTone.DIRETO -> "Perfeito, combinado. Às 15h estou no local combinado. Te vejo lá."
        AresTone.PROFISSIONAL -> "Prezado(a), acuso o recebimento das informações. Verifiquei os apontamentos e darei sequência ao procedimento solicitado. Atenciosamente."
        AresTone.NATURAL -> "Opa! Tudo certo por aqui. Vi sua mensagem e achei uma ótima ideia, vamos fazer isso sim!"
      }

      ScreenAnalysisResult(
        mode = mode,
        detectedApp = sampleTitle,
        detectedSpeaker = "Contato Recente",
        visibleMessages = visibleText.split("\n").filter { it.isNotBlank() },
        screenSummary = "Conversa identificada com sucesso. Mensagem final aguarda réplica.",
        suggestedReply = suggested,
        selectedTone = tone,
        isMockSample = true
      )
    }
  }
}
