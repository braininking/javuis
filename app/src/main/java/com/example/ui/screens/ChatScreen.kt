package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.gemini.GeminiRepository
import com.example.data.model.AresTone
import com.example.data.model.AssistantHudState
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.components.AresMessageItem
import com.example.ui.components.AresReactorCore
import com.example.ui.components.AresToneSelector
import com.example.ui.theme.AresBgDark
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCardSurface
import com.example.ui.theme.AresCyanGlow
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresSurfaceDark
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary
import com.example.voice.AresVoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
  geminiRepository: GeminiRepository,
  voiceManager: AresVoiceManager,
  onNavigateToVision: () -> Unit,
  onNavigateToKeyboard: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var inputText by remember { mutableStateOf("") }
  var selectedTone by remember { mutableStateOf(AresTone.NATURAL) }
  var hudState by remember { mutableStateOf(AssistantHudState.STANDBY) }
  var isProcessing by remember { mutableStateOf(false) }

  val voiceState by voiceManager.voiceState.collectAsState()
  var audioRms by remember { mutableStateOf(0f) }

  val messages = remember {
    mutableStateListOf(
      ChatMessage(
        sender = MessageSender.ARES,
        content = "Protocolo ARES ativado. Bom dia, senhor. Meus subsistemas neurais estão prontos. Você pode conversar comigo por voz ou texto, selecionar o tom das respostas ou utilizar a Visão Inteligente para responder a mensagens do WhatsApp e Instagram.",
        tone = AresTone.NATURAL
      )
    )
  }

  // Voice permission launcher
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      voiceManager.startListening()
    } else {
      Toast.makeText(context, "Permissão de microfone necessária para comandos de voz", Toast.LENGTH_SHORT).show()
    }
  }

  // Voice State handler
  LaunchedEffect(voiceState) {
    when (val state = voiceState) {
      is VoiceState.Listening -> {
        hudState = AssistantHudState.LISTENING_VOICE
        audioRms = state.rmsDb
      }
      is VoiceState.Ready -> {
        hudState = AssistantHudState.LISTENING_VOICE
      }
      is VoiceState.Success -> {
        hudState = AssistantHudState.STANDBY
        audioRms = 0f
        inputText = state.recognizedText
        // Auto send voice command
        if (state.recognizedText.isNotBlank()) {
          val userMsg = ChatMessage(
            sender = MessageSender.USER,
            content = state.recognizedText,
            isVoiceInput = true
          )
          messages.add(userMsg)
          inputText = ""
          isProcessing = true
          hudState = AssistantHudState.PROCESSING

          coroutineScope.launch {
            val replyResult = geminiRepository.generateChatReply(
              prompt = state.recognizedText,
              tone = selectedTone
            )
            val replyText = replyResult.getOrDefault("Desculpe, não consegui processar a resposta neste momento.")
            messages.add(
              ChatMessage(
                sender = MessageSender.ARES,
                content = replyText,
                tone = selectedTone
              )
            )
            isProcessing = false
            hudState = AssistantHudState.STANDBY
            listState.animateScrollToItem(messages.size - 1)
          }
        }
      }
      is VoiceState.Error -> {
        hudState = AssistantHudState.STANDBY
        audioRms = 0f
        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
      }
      is VoiceState.Idle -> {
        if (!isProcessing) {
          hudState = AssistantHudState.STANDBY
        }
        audioRms = 0f
      }
    }
  }

  val onSendMessage: () -> Unit = {
    val textToSend = inputText.trim()
    if (textToSend.isNotBlank() && !isProcessing) {
      val userMsg = ChatMessage(
        sender = MessageSender.USER,
        content = textToSend
      )
      messages.add(userMsg)
      inputText = ""
      isProcessing = true
      hudState = AssistantHudState.PROCESSING

      coroutineScope.launch {
        listState.animateScrollToItem(messages.size - 1)
        val replyResult = geminiRepository.generateChatReply(
          prompt = textToSend,
          tone = selectedTone
        )
        val replyText = replyResult.getOrDefault("Não consegui formular uma resposta.")
        messages.add(
          ChatMessage(
            sender = MessageSender.ARES,
            content = replyText,
            tone = selectedTone
          )
        )
        isProcessing = false
        hudState = AssistantHudState.STANDBY
        listState.animateScrollToItem(messages.size - 1)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AresBgDark)
  ) {
    // 1. Futuristic Arc Reactor Center
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      AresReactorCore(
        hudState = hudState,
        audioRms = audioRms,
        onCoreClick = {
          if (voiceState is VoiceState.Listening) {
            voiceManager.stopListening()
          } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
              voiceManager.startListening()
            } else {
              micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          }
        }
      )
    }

    // 2. Tone Selector
    AresToneSelector(
      selectedTone = selectedTone,
      onToneSelected = { selectedTone = it }
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 3. Quick Action Prompts
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      item {
        QuickActionChip(
          title = "👁️ Visão de Tela",
          subtitle = "Ler conversas",
          onClick = onNavigateToVision
        )
      }
      item {
        QuickActionChip(
          title = "⌨️ Teclado ARES",
          subtitle = "Ativar no WhatsApp",
          onClick = onNavigateToKeyboard
        )
      }
      item {
        QuickActionChip(
          title = "✨ Resposta Rápida",
          subtitle = "Como responder convite?",
          onClick = {
            inputText = "Alguém me convidou para sair no sábado mas já tenho compromisso. Como responder?"
            onSendMessage()
          }
        )
      }
      item {
        QuickActionChip(
          title = "💼 Tom Executivo",
          subtitle = "Cobrar pendência",
          onClick = {
            selectedTone = AresTone.PROFISSIONAL
            inputText = "Como cobrar um cliente educadamente sobre o pagamento em atraso?"
            onSendMessage()
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 4. Message History
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
      ) {
        items(messages, key = { it.id }) { msg ->
          AresMessageItem(message = msg)
        }

        if (isProcessing) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = AresCyanNeon,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "ARES articulando resposta no tom ${selectedTone.displayName}...",
                color = AresCyanNeon,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      // Voice recording overlay indicator
      if (voiceState is VoiceState.Listening) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60D1A2D))
            .border(1.5.dp, AresCyanNeon, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Gravando",
                tint = AresCyanNeon,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Escutando comando de voz em Português...",
                color = AresTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }

            IconButton(
              onClick = { voiceManager.stopListening() },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Parar Gravação",
                tint = AresCyanNeon
              )
            }
          }
        }
      }
    }

    // 5. Input Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(AresSurfaceDark)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Clear Chat Button
      IconButton(
        onClick = {
          messages.clear()
          messages.add(
            ChatMessage(
              sender = MessageSender.ARES,
              content = "Histórico redefinido. Subsistema ARES pronto para uma nova instrução.",
              tone = AresTone.NATURAL
            )
          )
        },
        modifier = Modifier.size(42.dp).testTag("clear_chat_button")
      ) {
        Icon(
          imageVector = Icons.Default.CleaningServices,
          contentDescription = "Limpar Histórico",
          tint = AresTextMuted,
          modifier = Modifier.size(20.dp)
        )
      }

      // Voice Button
      IconButton(
        onClick = {
          if (voiceState is VoiceState.Listening) {
            voiceManager.stopListening()
          } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
              voiceManager.startListening()
            } else {
              micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          }
        },
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(if (voiceState is VoiceState.Listening) AresCyanGlow else Color.Transparent)
          .testTag("voice_mic_button")
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Comando de Voz",
          tint = if (voiceState is VoiceState.Listening) AresCyanNeon else AresTextSecondary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Input TextField
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = {
          Text(
            text = "Digite uma mensagem ou peça uma resposta...",
            color = AresTextMuted,
            fontSize = 13.sp
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("chat_text_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = AresCardSurface,
          unfocusedContainerColor = AresCardSurface,
          focusedBorderColor = AresCyanNeon,
          unfocusedBorderColor = AresCardBorder,
          focusedTextColor = AresTextPrimary,
          unfocusedTextColor = AresTextPrimary
        ),
        shape = RoundedCornerShape(22.dp),
        maxLines = 4
      )

      Spacer(modifier = Modifier.width(6.dp))

      // Send Button
      IconButton(
        onClick = onSendMessage,
        enabled = inputText.isNotBlank() && !isProcessing,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(if (inputText.isNotBlank()) AresCyanNeon else AresCardSurface)
          .testTag("send_message_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Enviar Mensagem",
          tint = if (inputText.isNotBlank()) AresBgDark else AresTextMuted,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Composable
fun QuickActionChip(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(AresCardSurface)
      .border(1.dp, AresCardBorder, RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Column {
      Text(
        text = title,
        color = AresCyanNeon,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        color = AresTextMuted,
        fontSize = 10.sp
      )
    }
  }
}
