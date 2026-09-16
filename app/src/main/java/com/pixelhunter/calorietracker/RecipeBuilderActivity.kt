package com.pixelhunter.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

class RecipeBuilderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KaloriDarkTheme { RecipeBuilderScreen() } }
    }
}

data class RecipeIngredient(val food: CatalogFood, val grams: Double)

@Composable
private fun RecipeBuilderScreen() {
    val context = LocalContext.current
    val vm: TrackerViewModel = viewModel()
    var recipeName by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<RecipeIngredient>>(emptyList()) }
    var selecting by remember { mutableStateOf<CatalogFood?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); message = null }
    }

    val calories = ingredients.sumOf { it.food.calories100g * it.grams / 100.0 }
    val protein = ingredients.sumOf { it.food.protein100g * it.grams / 100.0 }
    val carbs = ingredients.sumOf { it.food.carbs100g * it.grams / 100.0 }
    val fat = ingredients.sumOf { it.food.fat100g * it.grams / 100.0 }
    val totalGrams = ingredients.sumOf { it.grams }
    val results = TurkishFoodCatalog.foods.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }.take(12)

    Scaffold(
        containerColor = KaloriBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Tarif / Öğün Oluştur", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KaloriBackground, titleContentColor = KaloriText)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2A1D)), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = KaloriGreen)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Fotoğraftan AI Analiz", fontWeight = FontWeight.Bold)
                                Text("Yemeğin fotoğrafından porsiyon ve makro tahmini al", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = { context.startActivity(Intent(context, PhotoMealAnalysisActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fotoğraf seç ve analiz et")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it },
                    label = { Text("Öğün / tarif adı") },
                    placeholder = { Text("Örn. Spor sonrası kahvaltım") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = KaloriSurface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Toplam", color = KaloriMuted)
                        Text("${calories.toInt()} kcal", style = MaterialTheme.typography.headlineMedium, color = KaloriGreen, fontWeight = FontWeight.Black)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Protein ${protein.toInt()} g")
                            Text("Karb ${carbs.toInt()} g")
                            Text("Yağ ${fat.toInt()} g")
                        }
                        Text("Toplam ${totalGrams.toInt()} g", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (ingredients.isNotEmpty()) {
                item { Text("İçindekiler", fontWeight = FontWeight.Bold) }
                items(ingredients, key = { it.food.name + it.grams }) { ingredient ->
                    Card(colors = CardDefaults.cardColors(containerColor = KaloriSurfaceAlt)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ingredient.food.name, fontWeight = FontWeight.Bold)
                                Text("${ingredient.grams.toInt()} g • ${(ingredient.food.calories100g * ingredient.grams / 100).toInt()} kcal", color = KaloriMuted)
                            }
                            IconButton(onClick = { ingredients = ingredients - ingredient }) {
                                Icon(Icons.Filled.RemoveCircleOutline, "Çıkar", tint = KaloriDanger)
                            }
                        }
                    }
                }
            }

            item {
                Text("Malzeme ekle", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Yiyecek ara") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(results, key = { it.name }) { food ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(food.name)
                        Text("100 g • ${food.calories100g.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { selecting = food }) {
                        Icon(Icons.Filled.AddCircle, "Ekle", tint = KaloriGreen)
                    }
                }
            }

            item {
                Button(
                    enabled = recipeName.isNotBlank() && ingredients.isNotEmpty(),
                    onClick = {
                        vm.saveMeal(recipeName.trim(), ingredients)
                        message = "Öğün kaydedildi"
                        recipeName = ""
                        query = ""
                        ingredients = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Filled.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Öğünü kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    selecting?.let { food ->
        RecipeAmountDialog(food, onDismiss = { selecting = null }) { grams ->
            ingredients = ingredients + RecipeIngredient(food, grams)
            selecting = null
        }
    }
}

@Composable
private fun RecipeAmountDialog(food: CatalogFood, onDismiss: () -> Unit, onAdd: (Double) -> Unit) {
    var grams by remember { mutableStateOf("100") }
    val value = grams.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    grams,
                    { grams = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') },
                    label = { Text("Gram") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("${(food.calories100g * value / 100).toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(enabled = value > 0, onClick = { onAdd(value) }) { Text("Ekle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
