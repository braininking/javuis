package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.service.AresContextBridge
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
import com.example.ui.theme.AresWarningAmber

@Composable
fun KeyboardSetupScreen(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var testInputText by remember { mutableStateOf("") }
  var isKeyboardEnabled by remember { mutableStateOf(AresContextBridge.isKeyboardEnabled(context)) }

  val refreshStatus = {
    isKeyboardEnabled = AresContextBridge.isKeyboardEnabled(context)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AresBgDark)
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("keyboard_setup_screen")
  ) {
    // Top Title & Icon
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
            imageVector = Icons.Default.Keyboard,
            contentDescription = "Teclado ARES",
            tint = AresCyanNeon,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "TECLADO INTELIGENTE ARES",
            color = AresCyanNeon,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )
          Text(
            text = "InputMethodService nativo com barra de IA",
            color = AresTextSecondary,
            fontSize = 12.sp
          )
        }
      }

      IconButton(
        onClick = { refreshStatus() },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Atualizar status",
          tint = AresTextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Real Status Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(AresCardSurface)
        .border(
          width = 1.dp,
          color = if (isKeyboardEnabled) AresSuccessGreen else AresWarningAmber,
          shape = RoundedCornerShape(12.dp)
        )
        .padding(16.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isKeyboardEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
          contentDescription = null,
          tint = if (isKeyboardEnabled) AresSuccessGreen else AresWarningAmber,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (isKeyboardEnabled) "TECLADO ARES HABILITADO NO SISTEMA" else "TECLADO REQUER ATIVAÇÃO NAS CONFIGURAÇÕES",
            color = if (isKeyboardEnabled) AresSuccessGreen else AresWarningAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = if (isKeyboardEnabled)
              "O serviço do Teclado ARES está ativo. Você já pode selecioná-lo para digitar em qualquer app."
            else
              "No Android, teclados de terceiros precisam ser habilitados pelo usuário em Configurações > Teclados.",
            color = AresTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Activation Steps
    Text(
      text = "PASSOS DE CONFIGURAÇÃO",
      color = AresTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Step 1: Open Settings
    SetupStepCard(
      stepNumber = "1",
      title = "Habilitar Teclado ARES nos Ajustes",
      description = "Abre a tela do Android onde você ativa a chave do 'Teclado Inteligente ARES'.",
      actionText = "Abrir Ajustes de Teclado",
      onAction = {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        context.startActivity(intent)
      }
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Step 2: Select Keyboard as Active
    SetupStepCard(
      stepNumber = "2",
      title = "Alternar Teclado Atual",
      description = "Abre o seletor do Android para escolher o ARES como teclado de entrada padrão.",
      actionText = "Selecionar Teclado",
      onAction = {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
      }
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Interactive Typing Test Sandbox
    Text(
      text = "LABORATÓRIO DE DIGITAÇÃO E TESTE DE IA",
      color = AresTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Toque no campo abaixo para abrir o teclado e testar a barra de IA do ARES, troca de tons e inserção de resposta:",
      color = AresTextSecondary,
      fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = testInputText,
      onValueChange = { testInputText = it },
      placeholder = {
        Text("Toque aqui para subir o teclado e testar...", color = AresTextMuted, fontSize = 13.sp)
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("keyboard_test_sandbox_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = AresCardSurface,
        unfocusedContainerColor = AresCardSurface,
        focusedBorderColor = AresCyanNeon,
        unfocusedBorderColor = AresCardBorder,
        focusedTextColor = AresTextPrimary,
        unfocusedTextColor = AresTextPrimary
      ),
      shape = RoundedCornerShape(10.dp),
      minLines = 3
    )

    if (testInputText.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = { testInputText = "" },
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = AresTextMuted)
        ) {
          Text("Limpar Campo de Teste", fontSize = 11.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Samsung Galaxy A36 & A72 Specific Guide
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(AresSurfaceDark)
        .border(1.dp, AresCardBorder, RoundedCornerShape(12.dp))
        .padding(16.dp)
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = "Samsung Galaxy",
            tint = AresBlueElectric,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "GUIA SAMSUNG GALAXY A36 & GALAXY A72 (ONE UI)",
            color = AresBlueElectric,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "• Botão de Teclado na Barra de Navegação: Na One UI da Samsung, ative 'Botão de Teclado na Barra de Navegação' nas configurações do sistema para alternar facilmente entre o Teclado Samsung e o ARES com 1 toque.\n• Otimização de Bateria: No Galaxy A36 e A72, certifique-se de definir o ARES como 'Não Otimizado' no gerenciamento de bateria da One UI para que o serviço do teclado nunca seja suspenso pelo sistema em segundo plano.\n• Taxa de Atualização: O layout do teclado foi compilado com otimização para telas de 90Hz e 120Hz dos painéis Super AMOLED da Samsung.",
          color = AresTextSecondary,
          fontSize = 11.sp,
          lineHeight = 17.sp
        )
      }
    }
  }
}

@Composable
fun SetupStepCard(
  stepNumber: String,
  title: String,
  description: String,
  actionText: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(AresCardSurface)
      .border(1.dp, AresCardBorder, RoundedCornerShape(10.dp))
      .padding(14.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(AresKeyAccentBg),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = stepNumber,
            color = AresCyanNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = title,
          color = AresTextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = description,
        color = AresTextSecondary,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = onAction,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = AresCyanNeon,
          contentColor = AresBgDark
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
