package com.handy.moodtracker

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.handy.moodtracker.ui.theme.MoodTrackerTheme
import kotlinx.coroutines.*
import org.ocpsoft.prettytime.PrettyTime
import java.time.ZoneId
import java.util.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val emotionToEmoji = mapOf(
    "Miedo" to "😱",
    "Vergüenza" to "😳",
    "Orgullo" to "😤",
    "Rechazo" to "🚫",
    "Abandono" to "🏚️",
    "Culpa" to "😔",
    "Víctima" to "🎭",
    "Perseguidor" to "👿",
    "Salvador" to "🦸"
)

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = DBHelper(this)
        setContent {
            MoodTrackerTheme {
                // Get the background color of the theme
                val backgroundColor = MaterialTheme.colorScheme.background
                // Set the status bar color to match the background color
                window.statusBarColor = backgroundColor.toArgb()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor

                ) {
                    MainContent(dbHelper)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainContent(dbHelper: DBHelper) {
    val snackbarHostState = remember { SnackbarHostState() }
    var emotions by remember { mutableStateOf(listOf<EmotionRecord>()) }

    LaunchedEffect(key1 = true) {
        emotions = dbHelper.readAllEmotions()
    }
    val updateEmotions = {
        emotions = dbHelper.readAllEmotions()
    }

    Scaffold(

    ) {
        Box() { // Add padding at the top
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmotionButtons(dbHelper, snackbarHostState, updateEmotions)
                Spacer(modifier = Modifier.height(20.dp))
                EmotionList(emotions, dbHelper, updateEmotions)
            }
        }
    }
}

data class Emotion(val name: String, val emoji: String)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EmotionButtons(
    dbHelper: DBHelper,
    snackbarHostState: SnackbarHostState,
    updateEmotions: () -> Unit
) {
    val context = LocalContext.current
    val emotions = listOf(
        listOf(Emotion("Miedo", "😱"), Emotion("Vergüenza", "😳"), Emotion("Orgullo", "😤")),
        listOf(Emotion("Rechazo", "🚫"), Emotion("Abandono", "🏚️"), Emotion("Culpa", "😔")),
        listOf(Emotion("Víctima", "🎭"), Emotion("Perseguidor", "👿"), Emotion("Salvador", "🦸"))
    )
    val titles = listOf("Negaciones", "Emociones", "Roles")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        emotions.forEachIndexed { index, emotionGroup ->
            Text(titles[index], style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                emotionGroup.forEach { emotion ->
                    val emotionCount = dbHelper.getEmotionCount(emotion.name)
                    val scale by animateFloatAsState(if (emotionCount > 0) 0.9f else 1f)
                    Button(
                        onClick = {
                            handleButtonClick(
                                emotion.name,
                                dbHelper,
                                snackbarHostState,
                                context,
                                updateEmotions
                            )
                        },
                        shape = RoundedCornerShape(3.dp), // Less rounded corners
                        modifier = Modifier
                            .padding(2.dp) // Less padding
                            .padding(horizontal = 0.dp) // Remove horizontal padding
                            .scale(scale) // Add scale animation
                    ) {
                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${emotion.emoji}${emotion.name}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically) // Align text to center vertically
                                    .padding(end = 2.dp) // Reduce padding to the end of the text
                            ) // Smaller text
                            Box(
                                modifier = Modifier
                                    .background(if (emotionCount > 0) Color(0xFFFFA500) else Color(0xFF008000)) // Orange if count > 0, otherwise green
                                    .clip(RoundedCornerShape(100))
                                    .padding(horizontal = 1.dp, vertical = 1.dp), // Reduce padding
                                contentAlignment = Alignment.Center
                            ) {
                                Text( // Fill the width of the box
                                    "$emotionCount",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (emotionCount > 0) Color.Black else Color.White) // White text if count is 0, otherwise black
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun handleButtonClick(
    emotion: String,
    dbHelper: DBHelper,
    snackbarHostState: SnackbarHostState,
    context: Context,
    updateEmotions: () -> Unit
) {
    dbHelper.insertRecord(emotion)
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    updateEmotions()
    CoroutineScope(Dispatchers.Main).launch {
        snackbarHostState.showSnackbar("Recorded: $emotion")
    }
}

@Composable
fun EmotionList(emotions: List<EmotionRecord>, dbHelper: DBHelper, updateEmotions: () -> Unit) {
    val debouncer = remember { Debouncer(CoroutineScope(Dispatchers.IO), 300L) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(15.dp) // Add 10.dp of space between each item
    ) {
        items(emotions, key = { it.id }) { emotion ->
            var comment by remember { mutableStateOf(emotion.comments) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "${emotionToEmoji[emotion.text] ?: ""} ${emotion.text}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(emotion.timestamp, style = MaterialTheme.typography.bodySmall)
                    TextField(
                        value = comment,
                        textStyle = MaterialTheme.typography.bodySmall,
                        onValueChange = { updatedComment ->
                            comment = updatedComment
                            debouncer.debounce {
                                dbHelper.updateComment(emotion.id, comment)
                                updateEmotions()
                            }
                        },
                        modifier = Modifier
                            .height(80.dp) // Set a fixed height
                            .width(350.dp), // Set a fixed width
                        maxLines = 3 // Enable multiline input
                    )
                }
                IconButton(onClick = {
                    dbHelper.deleteRecord(emotion.id)
                    updateEmotions()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

data class EmotionRecord(val id: Int, val text: String, val timestamp: String, var comments: String)

class DBHelper(context: Context) : SQLiteOpenHelper(context, "moodtracker.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE Records (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT, timestamp TEXT DEFAULT CURRENT_TIMESTAMP, comments TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE Records ADD COLUMN comments TEXT DEFAULT ''") // Add the new column with default empty text
        }
    }

    fun getEmotionCount(emotion: String): Int {
        val cursor: Cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM Records WHERE text = '$emotion'",
            null
        )
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun deleteRecord(id: Int) {
        writableDatabase.execSQL("DELETE FROM Records WHERE id = $id")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertRecord(text: String, comments: String = "") {
        val currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        writableDatabase.execSQL("INSERT INTO Records (text, comments, timestamp) VALUES ('$text', '$comments', '$currentTimestamp')")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("Range")
    fun readAllEmotions(): List<EmotionRecord> {
        val prettyTime = PrettyTime(Locale("es"))
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val cursor: Cursor = readableDatabase.rawQuery(
            "SELECT id, text, timestamp, comments FROM Records ORDER BY timestamp DESC",
            null
        )
        val emotions = mutableListOf<EmotionRecord>()
        while (cursor.moveToNext()) {
            // Parse the timestamp using the formatter
            val localDateTime =
                LocalDateTime.parse(cursor.getString(cursor.getColumnIndex("timestamp")), formatter)
            val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())

            val formattedTime = prettyTime.format(Date.from(zonedDateTime.toInstant()))
            emotions.add(
                EmotionRecord(
                    cursor.getInt(cursor.getColumnIndex("id")),
                    cursor.getString(cursor.getColumnIndex("text")),
                    formattedTime,
                    cursor.getString(cursor.getColumnIndex("comments"))
                )
            )
        }
        cursor.close()
        return emotions
    }


    fun updateComment(id: Int, comments: String) {
        writableDatabase.execSQL("UPDATE Records SET comments = '$comments' WHERE id = $id")
    }
}

class Debouncer(val scope: CoroutineScope, private val delayMillis: Long) {
    private var job: Job? = null

    fun debounce(action: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            action()
        }
    }
}
