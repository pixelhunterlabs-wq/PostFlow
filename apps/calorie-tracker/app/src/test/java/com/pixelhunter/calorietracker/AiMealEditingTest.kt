package com.pixelhunter.calorietracker
import org.junit.Assert.assertEquals
import org.junit.Test
class AiMealEditingTest { @Test fun scalesMacrosFromOriginalValues() { val x=scaleAiMeal(EditableAiMeal(AiMealItem("Tavuk",100.0,165.0,31.0,0.0,3.6)),150.0); assertEquals(247.5,x.calories,0.01); assertEquals(46.5,x.proteinG,0.01) } }
