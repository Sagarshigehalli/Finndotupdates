package com.anomapro.finndot.ui.screens.analytics

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    modifier: Modifier = Modifier
) {
    // Production API key
    val apiKey = "zpka_9330864ac31649aca93af54f1effd7b4_50efa38b"
    val client = remember { OkHttpClient() }

    var mobile by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var webViewUrl by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showWebView) {
        // WebView Screen with close option
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Loan Application",
                            style = MaterialTheme.typography.titleMedium
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { 
                                showWebView = false
                                webViewUrl = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        loadUrl(webViewUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    } else {
        // Premium Form Screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Background Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Header Text
                Text(
                    text = "Get Instant Loan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "Fast • Secure • Paperless",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Main Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Enter Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Mobile Input
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { if (it.length <= 10) mobile = it },
                            label = { Text("Mobile Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // PAN Input
                        OutlinedTextField(
                            value = pan,
                            onValueChange = { if (it.length <= 10) pan = it.uppercase() },
                            label = { Text("PAN Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Button
                        val isFormValid = mobile.length == 10 && pan.length == 10
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        val responseText = sendCreateLeadRequest(client, apiKey, mobile, pan)
                                        Log.d("LoansScreen", "API Response: $responseText")
                                        
                                        val parts = responseText.split("\n\n")
                                        if (parts.size > 1) {
                                            val jsonBody = parts[1]
                                            if (jsonBody.contains("\"error\"")) {
                                                val jsonObject = JSONObject(jsonBody)
                                                if (jsonObject.has("error")) {
                                                    val errorObj = jsonObject.getJSONObject("error")
                                                    val errorDetail = if (errorObj.has("detail")) errorObj.getString("detail") else "Error creating lead"
                                                    Toast.makeText(context, errorDetail, Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                val jsonObject = JSONObject(jsonBody)
                                                var loginUrl: String? = null
                                                if (jsonObject.has("data")) {
                                                    val dataObj = jsonObject.getJSONObject("data")
                                                    if (dataObj.has("loginUrl")) loginUrl = dataObj.getString("loginUrl")
                                                }
                                                if (!loginUrl.isNullOrBlank()) {
                                                    webViewUrl = loginUrl
                                                    showWebView = true
                                                } else {
                                                    Toast.makeText(context, "Error: No login URL received", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Error: Invalid response", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message ?: "Failed"}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Check Eligibility",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Benefits Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BenefitItem(
                        icon = androidx.compose.material.icons.Icons.Default.CheckCircle,
                        text = "100% Digital"
                    )
                    BenefitItem(
                        icon = androidx.compose.material.icons.Icons.Default.ThumbUp,
                        text = "Instant Approval"
                    )
                    BenefitItem(
                        icon = androidx.compose.material.icons.Icons.Default.Lock,
                        text = "Secure"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Powered by AbhiLoans
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Powered by",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.anomapro.finndot.R.drawable.abhiloans_logo),
                        contentDescription = "AbhiLoans Logo",
                        modifier = Modifier.height(40.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

suspend fun sendCreateLeadRequest(
    client: OkHttpClient,
    apiKey: String,
    mobile: String,
    pan: String
): String = withContext(Dispatchers.IO) {
    try {
        val jsonBody = """
            {
              "provider": "MFC",
              "mobileNumber": "$mobile",
              "panNumber": "$pan",
              "subPartnerCode": "sub123",
              "sendEmailToCustomer": true,
              "loanPurpose": "Education",
              "redirectionUrl": "https://www.anomapro.com",
              "utmSource": "google",
              "emailAddress": "akshay@anomapro.com"
            }
        """.trimIndent()

        // Print request body
        Log.d("LoansScreen", "Request Body: $jsonBody")

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
        val request = Request.Builder()
            .url("https://api.partners.abhiloans.com/v2/leads")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "No Response"
            val fullResponse = "Status: ${response.code}\n\n$body"
            
            // Print response to log
            Log.d("LoansScreen", "HTTP Status: ${response.code}")
            Log.d("LoansScreen", "Response Body: $body")
            Log.d("LoansScreen", "Full Response: $fullResponse")
            
            fullResponse
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${e.message ?: e.javaClass.simpleName}"
    }
}
