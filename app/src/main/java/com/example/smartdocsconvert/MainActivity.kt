package com.example.smartdocsconvert

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartdocsconvert.ui.navigation.Screen
import com.example.smartdocsconvert.ui.screens.ToolsScreen
import com.example.smartdocsconvert.ui.screens.HomeScreen
import com.example.smartdocsconvert.ui.screens.AddFileScreen
import com.example.smartdocsconvert.ui.screens.ImageEditorScreen
import com.example.smartdocsconvert.ui.screens.ImageFilterScreen
import com.example.smartdocsconvert.ui.screens.ConvertFileScreen
import com.example.smartdocsconvert.ui.theme.SmartDocsConvertTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartDocsConvertTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenFile = { navController.navigate(Screen.ConvertFile.route) },
                onOpenCamera = { /* TODO */ },
                onOpenGallery = { navController.navigate(Screen.AddFile.route) }
            )
        }
        composable(Screen.AddFile.route) {
            AddFileScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                navController = navController
            )
        }
        composable(Screen.ConvertFile.route) {
            ConvertFileScreen(
                onBackClick = { navController.navigateUp() }
            )
        }
        composable(
            route = "image_editor/{imageUri}",
            arguments = listOf(
                navArgument("imageUri") { 
                    type = NavType.StringType
                    nullable = false 
                }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val uris = imageUri.split(",").map { encodedUri ->
                Uri.parse(URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString()))
            }
            Box(modifier = Modifier.fillMaxSize()) {
                ModalBottomSheet(
                    onDismissRequest = { navController.navigateUp() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = null
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ImageEditorScreen(
                            navController = navController,
                            imageUris = uris
                        )
                    }
                }
            }
        }
        composable(
            route = "image_filter/{imageUri}",
            arguments = listOf(
                navArgument("imageUri") { 
                    type = NavType.StringType
                    nullable = false 
                }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val uris = imageUri.split(",").map { encodedUri ->
                Uri.parse(URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString()))
            }
            Box(modifier = Modifier.fillMaxSize()) {
                ModalBottomSheet(
                    onDismissRequest = { navController.navigateUp() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = null
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ImageFilterScreen(
                            navController = navController,
                            imageUris = uris
                        )
                    }
                }
            }
        }
    }
}
