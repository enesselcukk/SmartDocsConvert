package com.example.smartdocsconvert.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smartdocsconvert.ui.screens.HomeScreen
import com.example.smartdocsconvert.ui.screens.file.ConvertFileScreen
import com.example.smartdocsconvert.ui.screens.file.EditOptimizeScreen
import com.example.smartdocsconvert.ui.screens.file.SaveShareScreen
import com.example.smartdocsconvert.ui.screens.image.FilterScreen
import com.example.smartdocsconvert.ui.screens.image.SelectPermissionsScreen
import com.example.smartdocsconvert.ui.screens.image.SelectedImageScreen
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(
    navController: NavHostController, startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController, startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onOpenFile = {
                navController.navigate(Screen.FileConverter.route)
            }, onOpenGallery = {
                navController.navigate(Screen.SelectPermissions.route)
            },
                navController = navController)
        }

        composable(Screen.SelectPermissions.route) {
            SelectPermissionsScreen(selectedImageNavigator = { imageUrisString ->
                navController.navigate(Screen.SelectedImage.selectedImageFilterRoute(imageUrisString))
            }, navigateUp = {
                navController.navigateUp()
            })
        }

        composable(route = "${Screen.SelectedImage.route}/{imageUris}",
            arguments = listOf(
                navArgument("imageUris") {
                    type = NavType.StringType
                }
            )) { backStackEntry ->
            val imageUrisString = backStackEntry.arguments?.getString("imageUris")
            val imageUris = imageUrisString?.split(",")?.map { uriString ->
                Uri.parse(uriString)
            } ?: emptyList()

            SelectedImageScreen(
                selectedImage = { selectedImagesString ->
                    navController.navigate(Screen.Filter.createRoute(selectedImagesString))
                }, 
                navigateUp = {
                    navController.navigateUp()
                }, 
                imageUris = imageUris
            )
        }

        composable(
            route = "${Screen.Filter.route}/{selectedImages}",
            arguments = listOf(
                navArgument("selectedImages") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val selectedImagesString = backStackEntry.arguments?.getString("selectedImages")
            val selectedImageUris = selectedImagesString?.split(",")?.map { uriString ->
                Uri.parse(uriString)
            } ?: emptyList()
            
            FilterScreen(
                navigateUp = {
                    navController.navigate(Screen.SelectPermissions.route){
                        popUpTo(Screen.Home.route){
                            inclusive = false
                        }
                    } 
                },
                imageUris = selectedImageUris
            )
        }

        composable(route = Screen.FileConverter.route) {
            ConvertFileScreen(
                onBackClick = {
                    navController.navigateUp()
                }, 
                onNextClick = { selectedFiles ->
                    val filePathsString = selectedFiles.joinToString(",") { it.absolutePath }
                    navController.navigate(Screen.EditOptimize.createRoute(filePathsString))
                }
            )
        }
        
        composable(
            route = "${Screen.EditOptimize.route}/{filePaths}",
            arguments = listOf(
                navArgument("filePaths") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val filePathsString = backStackEntry.arguments?.getString("filePaths")
            val decodedPaths = filePathsString?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            
            val selectedFiles = decodedPaths.split(",").map { path ->
                File(path)
            }
            
            EditOptimizeScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNextClick = { optimizedFiles ->
                    navController.navigate(Screen.SaveShare.createRoute(optimizedFiles))
                },
                selectedFiles = selectedFiles
            )
        }

        composable(
            route = "${Screen.SaveShare.route}/{optimizedFiles}",
            arguments = listOf(
                navArgument("optimizedFiles") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val optimizedFilesString = backStackEntry.arguments?.getString("optimizedFiles")
            val decodedPaths = optimizedFilesString?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            
            val optimizedFiles = decodedPaths.split(",").map { path ->
                File(path)
            }
            
            SaveShareScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onFinish = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                },
                optimizedFiles = optimizedFiles
            )
        }
    }
} 