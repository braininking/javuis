package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.gemini.GeminiRepository
import com.example.service.AresContextBridge
import com.example.ui.theme.AresBgDark
import com.example.ui.theme.AresBlueElectric
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCardSurface
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresErrorRed
import com.example.ui.theme.AresKeyAccentBg
import com.example.ui.theme.AresSuccessGreen
import com.example.ui.theme.AresSurfaceDark
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary
import com.example.ui.theme.AresWarningAmber
import kotlinx.coroutines.launch

@Composable
fun SystemStatusScreen(
  geminiRepository: GeminiRepository,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  var hasMicPermission by remember {
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
  }
  var isAccessibilityEnabled by remember { mutableStateOf(AresContextBridge.isAccessibilityServiceEnabled(context)) }
  var isKeyboardEnabled by remember { mutableStateOf(AresContextBridge.isKeyboardEnabled(context)) }

  var customKeyInput by remember { mutableStateOf(geminiRepository.getApiKey()) }
  var isTestingConnection by remember { mutableStateOf(false) }
  var pingResult by remember { mutableStateOf<String?>(null) }

  val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasMicPermission = granted
    Toast.makeText(context, if (granted) "Permissão de microfone concedida!" else "Permissão negada", Toast.LENGTH_SHORT).show()
  }

  val testConnection: () -> Unit = {
    isTestingConnection = true
    pingResult = null
    coroutineScope.launch {
      val startTime = System.currentTimeMillis()
      val result = geminiRepository.generateChatReply("Ping de verificação de latência do sistema ARES.", com.example.data.model.AresTone.DIRETO)
      val duration = System.currentTimeMillis() - startTime
      isTestingConnection = false
      if (result.isSuccess) {
        val isCloud = geminiRepository.isCloudKeyConfigured()
        pingResult = if (isCloud) {
          "✓ Conexão com Gemini 3.8 Flash bem-sucedida! Latência: ${duration}ms"
        } else {
          "✓ Núcleo Neural Local ARES respondendo normalmente em ${duration}ms (Chave de nuvem não configurada)"
        }
      } else {
        pingResult = "Falha no teste: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AresBgDark)
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("system_status_screen")
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
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
            imageVector = Icons.Default.Terminal,
            contentDescription = "Diagnóstico",
            tint = AresCyanNeon,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "DIAGNÓSTICO DO SISTEMA ARES",
            color = AresCyanNeon,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
          Text(
            text = "Status da IA, credenciais e permissões",
            color = AresTextSecondary,
            fontSize = 12.sp
          )
        }
      }

      IconButton(
        onClick = {
          hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
          isAccessibilityEnabled = AresContextBridge.isAccessibilityServiceEnabled(context)
          isKeyboardEnabled = AresContextBridge.isKeyboardEnabled(context)
        }
      ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = AresTextSecondary)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Gemini Cloud API Card
    Text(
      text = "INTEGRAÇÃO GEMINI IA & CREDENCIAIS",
      color = AresTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(AresCardSurface)
        .border(1.dp, AresCardBorder, RoundedCornerShape(12.dp))
        .padding(16.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = null,
              tint = if (geminiRepository.isCloudKeyConfigured()) AresSuccessGreen else AresWarningAmber,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (geminiRepository.isCloudKeyConfigured()) "Chave Gemini Configurada" else "Modo Núcleo Local Ativo",
              color = AresTextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (geminiRepository.isCloudKeyConfigured()) AresKeyAccentBg else AresSurfaceDark)
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = if (geminiRepository.isCloudKeyConfigured()) "ONLINE" else "LOCAL",
              color = if (geminiRepository.isCloudKeyConfigured()) AresCyanNeon else AresWarningAmber,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (geminiRepository.isCloudKeyConfigured())
            "As requisições à IA estão sendo transmitidas com segurança à API do Google Gemini. A chave é protegida via BuildConfig e painel Secrets."
          else
            "Você pode configurar sua GEMINI_API_KEY no painel Secrets do AI Studio para ativar as chamadas de nuvem em tempo real, ou inserir uma chave personalizada abaixo:",
          color = AresTextSecondary,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = customKeyInput,
          onValueChange = {
            customKeyInput = it
            geminiRepository.setCustomApiKey(it)
          },
          label = { Text("Chave de API Gemini (opcional)", fontSize = 11.sp, color = AresTextMuted) },
          placeholder = { Text("AIzaSy...", color = AresTextMuted, fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AresSurfaceDark,
            unfocusedContainerColor = AresSurfaceDark,
            focusedBorderColor = AresCyanNeon,
            unfocusedBorderColor = AresCardBorder,
            focusedTextColor = AresTextPrimary,
            unfocusedTextColor = AresTextPrimary
          ),
          shape = RoundedCornerShape(8.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = { testConnection() },
          enabled = !isTestingConnection,
          modifier = Modifier.fillMaxWidth().height(42.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AresCyanNeon, contentColor = AresBgDark),
          shape = RoundedCornerShape(8.dp)
        ) {
          if (isTestingConnection) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AresBgDark, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verificando Latência...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          } else {
            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Testar Conexão / Ping ARES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        if (pingResult != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = pingResult!!,
            color = if (pingResult!!.startsWith("✓")) AresSuccessGreen else AresErrorRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Permissions Audit
    Text(
      text = "PERMISSÕES E SERVIÇOS DO ANDROID",
      color = AresTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Microphone
    PermissionRow(
      title = "Microfone (Comandos de Voz)",
      description = "Necessário para reconhecimento de fala em Português via SpeechRecognizer.",
      isGranted = hasMicPermission,
      onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
      actionLabel = "Conceder"
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Keyboard
    PermissionRow(
      title = "Teclado Android ARES",
      description = "InputMethodService nativo para digitação e barra de IA dentro do WhatsApp/Instagram.",
      isGranted = isKeyboardEnabled,
      onAction = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
      actionLabel = "Ajustes"
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Accessibility Service
    PermissionRow(
      title = "Acessibilidade (Leitor de Tela)",
      description = "Permite ao ARES ler nós de conversa visíveis para formular réplicas contextuais.",
      isGranted = isAccessibilityEnabled,
      onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
      actionLabel = "Acessibilidade"
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Honest Capabilities Matrix
    Text(
      text = "TRANSPARÊNCIA DO PROJETO ARES",
      color = AresTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(AresSurfaceDark)
        .border(1.dp, AresCardBorder, RoundedCornerShape(12.dp))
        .padding(14.dp)
    ) {
      Column {
        Text(
          text = "✓ 100% FUNCIONANDO NESTA VERSÃO (V1.0):",
          color = AresSuccessGreen,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "• Chat de IA com personalidade estilo JARVIS\n• Reconhecimento de voz em tempo real em Português\n• Teclado nativo InputMethodService completo (QWERTY + símbolos + barra de IA)\n• Inserção de respostas no campo de texto para revisão (sem envio forçado)\n• Visão Inteligente: Modo 1 (Descrever) e Modo 2 (Responder à Conversa)\n• Seletor de 6 tons estratégicos de resposta\n• Otimização para Samsung Galaxy A36 e Galaxy A72 One UI",
          color = AresTextPrimary,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "⏳ PLANEJADO PARA AS PRÓXIMAS FASES:",
          color = AresBlueElectric,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "• Bolha flutuante sobreposta (ChatHead) para acionar a Visão sem abrir o app\n• Integração direta com modelo local Gemini Nano para resposta 100% offline\n• Aprendizado contínuo do vocabulário do usuário",
          color = AresTextSecondary,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )
      }
    }
  }
}

@Composable
fun PermissionRow(
  title: String,
  description: String,
  isGranted: Boolean,
  onAction: () -> Unit,
  actionLabel: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(AresCardSurface)
      .border(1.dp, AresCardBorder, RoundedCornerShape(10.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) AresSuccessGreen else AresWarningAmber,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = title,
            color = AresTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          color = AresTextSecondary,
          fontSize = 11.sp,
          lineHeight = 15.sp
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      OutlinedButton(
        onClick = onAction,
        modifier = Modifier.height(36.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = if (isGranted) AresSuccessGreen else AresCyanNeon
        ),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(if (isGranted) "Ativo" else actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
