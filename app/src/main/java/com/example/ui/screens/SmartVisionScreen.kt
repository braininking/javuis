package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiRepository
import com.example.data.model.AresTone
import com.example.data.model.ScreenAnalysisResult
import com.example.data.model.ScreenVisionMode
import com.example.service.AresContextBridge
import com.example.ui.components.AresToneSelector
import com.example.ui.theme.AresBgDark
import com.example.ui.theme.AresBlueElectric
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCardSurface
import com.example.ui.theme.AresCyanGlow
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresKeyAccentBg
import com.example.ui.theme.AresSuccessGreen
import com.example.ui.theme.AresSurfaceDark
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SmartVisionScreen(
  geminiRepository: GeminiRepository,
  onNavigateToKeyboard: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  var selectedMode by remember { mutableStateOf(ScreenVisionMode.REPLY_CONVERSATION) }
  var selectedTone by remember { mutableStateOf(AresTone.NATURAL) }
  var isAnalyzing by remember { mutableStateOf(false) }

  val latestAccessibilityText by AresContextBridge.latestScreenText.collectAsState()
  val detectedAppPackage by AresContextBridge.detectedAppPackage.collectAsState()

  var customConversationText by remember {
    mutableStateOf(
      "WhatsApp (Marcos): Fala cara! Você viu o que o cliente mandou no e-mail? Precisamos fechar o relatório até as 16h para a apresentação com a diretoria. Você consegue revisar a sua parte?"
    )
  }

  var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var selectedImageName by remember { mutableStateOf<String?>(null) }

  var analysisResult by remember { mutableStateOf<ScreenAnalysisResult?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      try {
        val inputStream = context.contentResolver.openInputStream(it)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        selectedImageBitmap = bitmap
        selectedImageName = "Captura de tela selecionada (${bitmap.width}x${bitmap.height})"
        Toast.makeText(context, "Imagem carregada para análise multimodal do ARES!", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Erro ao carregar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val executeAnalysis: () -> Unit = {
    isAnalyzing = true
    coroutineScope.launch {
      val textToAnalyze = if (latestAccessibilityText.isNotBlank()) {
        "App: $detectedAppPackage\n$latestAccessibilityText"
      } else {
        customConversationText
      }

      val result = geminiRepository.analyzeScreenVision(
        mode = selectedMode,
        screenBitmap = selectedImageBitmap,
        contextualText = textToAnalyze,
        tone = selectedTone
      )

      analysisResult = result.getOrNull()
      isAnalyzing = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AresBgDark)
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("smart_vision_screen")
  ) {
    // Header Banner
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(AresSurfaceDark)
          .border(1.dp, AresCyanNeon, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Visibility,
          contentDescription = "Visão Inteligente",
          tint = AresCyanNeon,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = "VISÃO INTELIGENTE ARES",
          color = AresCyanNeon,
          fontSize = 16.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
        Text(
          text = "Interpretação multimodal de conversas na tela",
          color = AresTextSecondary,
          fontSize = 12.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Mode Selector Tab (Modo 1 vs Modo 2)
    TabRow(
      selectedTabIndex = if (selectedMode == ScreenVisionMode.DESCRIBE_SCREEN) 0 else 1,
      containerColor = AresSurfaceDark,
      contentColor = AresCyanNeon,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(
            tabPositions[if (selectedMode == ScreenVisionMode.DESCRIBE_SCREEN) 0 else 1]
          ),
          color = AresCyanNeon,
          height = 3.dp
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .border(1.dp, AresCardBorder, RoundedCornerShape(10.dp))
    ) {
      Tab(
        selected = selectedMode == ScreenVisionMode.DESCRIBE_SCREEN,
        onClick = { selectedMode = ScreenVisionMode.DESCRIBE_SCREEN },
        text = {
          Text(
            text = "Modo 1: Descrever Tela",
            fontWeight = if (selectedMode == ScreenVisionMode.DESCRIBE_SCREEN) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
          )
        }
      )
      Tab(
        selected = selectedMode == ScreenVisionMode.REPLY_CONVERSATION,
        onClick = { selectedMode = ScreenVisionMode.REPLY_CONVERSATION },
        text = {
          Text(
            text = "Modo 2: Responder Conversa",
            fontWeight = if (selectedMode == ScreenVisionMode.REPLY_CONVERSATION) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
          )
        }
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Mode Description
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(AresCardSurface)
        .padding(12.dp)
    ) {
      Row(verticalAlignment = Alignment.Top) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = "Informação",
          tint = AresBlueElectric,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (selectedMode == ScreenVisionMode.DESCRIBE_SCREEN) {
            "O ARES sintetiza o conteúdo da tela, identificando mensagens, participantes e assuntos principais sem gerar resposta de envio."
          } else {
            "O ARES analisa o contexto da conversa visível (ex: WhatsApp/Instagram) e formula uma réplica estratégica no tom que você pedir para você revisar antes de enviar."
          },
          color = AresTextSecondary,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )
      }
    }

    // Tone Selector (active for Mode 2)
    if (selectedMode == ScreenVisionMode.REPLY_CONVERSATION) {
      Spacer(modifier = Modifier.height(16.dp))
      AresToneSelector(
        selectedTone = selectedTone,
        onToneSelected = { selectedTone = it }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Source Selection Card
    Text(
      text = "FONTE DO CONTEXTO DA TELA",
      color = AresTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Live Accessibility Reader Status
    if (latestAccessibilityText.isNotBlank()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF0C2419))
          .border(1.dp, AresSuccessGreen, RoundedCornerShape(10.dp))
          .padding(12.dp)
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Ativo",
              tint = AresSuccessGreen,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Texto capturado ao vivo pelo ARES (${detectedAppPackage.ifBlank { "WhatsApp" }})",
              color = AresSuccessGreen,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = latestAccessibilityText.take(160) + if (latestAccessibilityText.length > 160) "..." else "",
            color = AresTextPrimary,
            fontSize = 12.sp
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    // Image Picker & Preview
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = { photoPickerLauncher.launch("image/*") },
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .testTag("pick_screenshot_button"),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = AresCyanNeon
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Image,
          contentDescription = "Carregar Screenshot",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Carregar Print", fontSize = 12.sp)
      }

      if (selectedImageBitmap != null) {
        OutlinedButton(
          onClick = {
            selectedImageBitmap = null
            selectedImageName = null
          },
          modifier = Modifier.height(44.dp),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = AresTextMuted)
        ) {
          Text("Limpar Imagem", fontSize = 11.sp)
        }
      }
    }

    if (selectedImageBitmap != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, AresCyanNeon, RoundedCornerShape(8.dp))
          .background(AresCardSurface),
        contentAlignment = Alignment.Center
      ) {
        Image(
          bitmap = selectedImageBitmap!!.asImageBitmap(),
          contentDescription = "Screenshot selecionado",
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Preset Sample Scenarios
    Text(
      text = "Ou teste com cenários reais de conversa:",
      color = AresTextSecondary,
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      PresetChatButton(
        title = "WhatsApp Trabalho",
        onClick = {
          customConversationText = "WhatsApp (Gerente Ana): Bom dia equipe! O cliente aprovou a proposta preliminar. Precisamos confirmar se conseguimos entregar na sexta-feira. Algum impedimento?"
        },
        modifier = Modifier.weight(1f)
      )
      PresetChatButton(
        title = "WhatsApp Amigo",
        onClick = {
          customConversationText = "WhatsApp (Lucas): E aí mano, tudo certo? Vai rolar aquele churrasco no domingo na casa do Pedro. Você vai levar o quê?"
        },
        modifier = Modifier.weight(1f)
      )
      PresetChatButton(
        title = "Instagram DM",
        onClick = {
          customConversationText = "Instagram Direct (@mari_design): Oi! Adorei seus posts recentes sobre tecnologia. Queria te convidar para participar de uma live na terça!"
        },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Editable Conversation Text
    OutlinedTextField(
      value = customConversationText,
      onValueChange = { customConversationText = it },
      label = { Text("Texto da Conversa / Contexto", color = AresTextMuted, fontSize = 12.sp) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("conversation_context_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = AresCardSurface,
        unfocusedContainerColor = AresCardSurface,
        focusedBorderColor = AresCyanNeon,
        unfocusedBorderColor = AresCardBorder,
        focusedTextColor = AresTextPrimary,
        unfocusedTextColor = AresTextPrimary
      ),
      shape = RoundedCornerShape(8.dp),
      maxLines = 4
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Action Trigger Button
    Button(
      onClick = { executeAnalysis() },
      enabled = !isAnalyzing,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("execute_vision_button"),
      colors = ButtonDefaults.buttonColors(
        containerColor = AresCyanNeon,
        contentColor = AresBgDark
      ),
      shape = RoundedCornerShape(12.dp)
    ) {
      if (isAnalyzing) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AresBgDark, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text("ARES Processando Visão Multimodal...", fontWeight = FontWeight.Bold)
      } else {
        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (selectedMode == ScreenVisionMode.DESCRIBE_SCREEN) "ANALISAR E DESCREVER TELA" else "INTERPRETAR E GERAR RESPOSTA",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          letterSpacing = 0.5.sp
        )
      }
    }

    // Analysis Result Display
    if (analysisResult != null) {
      val res = analysisResult!!
      Spacer(modifier = Modifier.height(20.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(AresCardSurface)
          .border(1.5.dp, AresCyanNeon, RoundedCornerShape(12.dp))
          .padding(16.dp)
          .testTag("vision_result_card")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AresCyanNeon)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = res.detectedApp,
              color = AresCyanNeon,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(AresKeyAccentBg)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Tom: ${res.selectedTone.displayName}",
              color = AresTextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedMode == ScreenVisionMode.DESCRIBE_SCREEN) {
          Text(
            text = "SÍNTESE DA TELA:",
            color = AresTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = res.screenSummary,
            color = AresTextPrimary,
            fontSize = 13.sp,
            lineHeight = 19.sp
          )
        } else {
          Text(
            text = "RESPOSTA PRONTA PARA REVISÃO:",
            color = AresTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(AresSurfaceDark)
              .border(1.dp, AresCardBorder, RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Text(
              text = res.suggestedReply,
              color = AresTextPrimary,
              fontSize = 14.sp,
              lineHeight = 21.sp,
              fontWeight = FontWeight.Medium
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Decision Actions (Review, Insert, Copy)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                AresContextBridge.queueTextForKeyboard(res.suggestedReply)
                Toast.makeText(context, "Resposta enviada para o Teclado ARES! Abra o WhatsApp e toque em 'Inserir'.", Toast.LENGTH_LONG).show()
                onNavigateToKeyboard()
              },
              modifier = Modifier
                .weight(1.3f)
                .height(42.dp)
                .testTag("send_to_keyboard_btn"),
              colors = ButtonDefaults.buttonColors(
                containerColor = AresCyanNeon,
                contentColor = AresBgDark
              ),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(imageVector = Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Inserir no Teclado", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ARES Resposta", res.suggestedReply)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .testTag("copy_reply_btn"),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AresTextPrimary
              ),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Copiar", fontSize = 12.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Privacy & Safe Review Architecture Explanation Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(AresSurfaceDark)
        .border(1.dp, AresCardBorder, RoundedCornerShape(10.dp))
        .padding(14.dp)
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Segurança",
            tint = AresCyanNeon,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "PROTOCOLO DE PRIVACIDADE E CONTROLE",
            color = AresCyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "• Sem Envio Automático: O ARES nunca despacha mensagens sem sua ação explícita. Ele prepara a sugestão para você revisar, editar ou apagar no campo de texto.\n• Três Mecanismos Nativos: Leitura via Acessibilidade autorizada, Captura de tela autorizada pelo sistema e Inserção direta via Teclado ARES.\n• Proteção de Dados: Telas protegidas contra prints (bancos/senhas) são respeitadas pelo Android.",
          color = AresTextSecondary,
          fontSize = 11.sp,
          lineHeight = 17.sp
        )
      }
    }
  }
}

@Composable
fun PresetChatButton(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(AresCardSurface)
      .border(1.dp, AresCardBorder, RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = AresTextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
