package com.example.zjulogin
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.zjulogin.ui.theme.ZjuloginTheme
import androidx.compose.material3.TextFieldDefaults
import com.example.zjulogin.LoginViewModel.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider


class MainActivity : ComponentActivity() {
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        // Load saved credentials
        UserCredentials.loadCredentials(applicationContext)
        loginViewModel.checkNetworkStatus(applicationContext)
        setContent {
            ZjuloginTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(loginViewModel, this)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = LoginViewModel(),
    context: Context = LocalContext.current
)
{
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("zjucst") }

    val onUsernameChanged: (String) -> Unit = { value ->
        UserCredentials.username = value
        username.value = value
    }

    val onPasswordChanged: (String) -> Unit = { value ->
        UserCredentials.password = value
        password.value = value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(
            text = "ZJUCST Login",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            softWrap = true,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(30.dp))

        val loginStatus by loginViewModel.loginStatus
        Text("当前状态：$loginStatus")

        Spacer(modifier = Modifier.height(24.dp))

        // input text
        loginViewModel.isUsernamePasswordInputEnabled.value?.let { value ->
            OutlinedTextField(
                value = username.value,
                onValueChange = onUsernameChanged,
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = value,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        loginViewModel.isUsernamePasswordInputEnabled.value?.let { value ->
            OutlinedTextField(
                value = password.value,
                onValueChange = onPasswordChanged,
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = value,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        // button
        loginViewModel.isLoginButtonEnabled.value?.let { value ->
            Button(
                onClick = {
                    UserCredentials.username = username.value
                    UserCredentials.password = password.value
                    loginViewModel.performLogin(context)
                },
                enabled = value,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        loginViewModel.isLogoutButtonEnabled.value?.let { value ->
            Button(
                onClick = {
                    loginViewModel.performLogout(context)
                },
                enabled = value,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Logout")
            }
        }
    }
}
