package com.starfleet.idle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starfleet.idle.data.GameState
import com.starfleet.idle.ui.theme.*

@Composable
fun QuestPanel(state: GameState, onClaim: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    val anyAvailable = state.activeQuests.any { it.completed && !it.claimed }
    val total = state.activeQuests.size
    val completed = state.activeQuests.count { it.completed }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (anyAvailable) NebulaPurple.copy(alpha = 0.2f) else CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 DAILY QUESTS",
                    color = CreditGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$completed / $total",
                    color = if (anyAvailable) ShieldGreen else TextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        if (expanded) "Hide" else "Show",
                        color = StarBlue, fontSize = 11.sp
                    )
                }
            }

            if (expanded) {
                state.activeQuests.forEachIndexed { index, quest ->
                    val template = quest.template()
                    val progress = quest.progress
                    val target = template.targetValue
                    val pct = (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = template.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = template.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = template.description, color = TextSecondary, fontSize = 10.sp)
                            LinearProgressIndicator(
                                progress = pct,
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = if (quest.completed) ShieldGreen else NebulaPurple,
                                trackColor = DeepSpace
                            )
                            Text(
                                text = "$progress / $target",
                                color = TextSecondary, fontSize = 9.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            quest.claimed -> {
                                Text("✓", color = ShieldGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            quest.completed -> {
                                Button(
                                    onClick = { onClaim(index) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("+${template.gemReward}💎", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                Text(
                                    text = "+${template.gemReward}💎",
                                    color = NebulaPurple,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
