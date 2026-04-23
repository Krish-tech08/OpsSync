package com.example.opssync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opssync.ui.state.UiState
import com.example.opssync.ui.theme.*
import com.example.opssync.viewmodel.AppViewModel

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit,
    onGitHubLogin: (String) -> Unit   // passes the URL to open in browser
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState    = viewModel.authState
    val gitHubState  = viewModel.gitHubUrlState

    LaunchedEffect(authState) {
        if (authState is UiState.Success) {
            viewModel.resetAuthState()
            onLoginSuccess()
        }
    }

    // When GitHub URL is ready, open it
    LaunchedEffect(gitHubState) {
        if (gitHubState is UiState.Success) {
            onGitHubLogin(gitHubState.data.url)
        }
    }

    Column(
        modifier              = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Default.Terminal,
            contentDescription = null,
            tint               = Primary,
            modifier           = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("OpsSync",  fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Primary)
        Text("Infrastructure Operations", fontSize = 14.sp, color = Secondary)

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            label         = { Text("Email") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Primary,
                unfocusedBorderColor = DividerColor
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value                = password,
            onValueChange        = { password = it },
            label                = { Text("Password") },
            modifier             = Modifier.fillMaxWidth(),
            singleLine           = true,
            visualTransformation = PasswordVisualTransformation(),
            shape                = RoundedCornerShape(10.dp),
            colors               = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Primary,
                unfocusedBorderColor = DividerColor
            )
        )

        Spacer(Modifier.height(24.dp))

        if (authState is UiState.Error) {
            Text(authState.message, color = ColorFailed, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
        }

        // Email/password login button
        Button(
            onClick  = { viewModel.login(email, password) },
            enabled  = email.isNotBlank() && password.isNotBlank() && authState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (authState is UiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Divider
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            Text("  or  ", fontSize = 12.sp, color = Secondary)
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        }

        Spacer(Modifier.height(16.dp))

        // GitHub login button
        OutlinedButton(
            onClick  = { viewModel.fetchGitHubOAuthUrl() },
            enabled  = gitHubState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(10.dp),
            border   = BorderStroke(1.dp, DividerColor)
        ) {
            if (gitHubState is UiState.Loading) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Code, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign in with GitHub", fontWeight = FontWeight.SemiBold, color = Primary)
            }
        }
    }
}
