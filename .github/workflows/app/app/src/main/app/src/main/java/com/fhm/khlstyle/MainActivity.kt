package com.fhm.khlstyle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

// --- МОДЕЛИ ДАННЫХ ---
data class HockeyMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val periodScores: String,
    val date: String,
    val status: String, // "LIVE", "ОКОНЧЕН", "14:30"
    val arena: String,
    val protocolEvents: List<String>
)

data class StandingRow(
    val rank: Int,
    val team: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val goalsDiff: String,
    val points: Int
)

data class PlayerStat(
    val rank: Int,
    val name: String,
    val team: String,
    val games: Int,
    val goals: Int,
    val assists: Int,
    val points: Int
)

data class NewsItem(
    val title: String,
    val date: String,
    val url: String
)

// --- ПАРСЕР САЙТА FHMOSCOW.COM ---
object FhmParser {
    private const val BASE_URL = "https://fhmoscow.com"

    suspend fun fetchLiveSiteData(birthYear: String, group: String): Triple<List<HockeyMatch>, List<StandingRow>, List<NewsItem>> {
        return withContext(Dispatchers.IO) {
            val parsedNews = mutableListOf<NewsItem>()
            var connectionStatus = "Подключено к fhmoscow.com"

            try {
                val doc = Jsoup.connect(BASE_URL)
                    .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                    .timeout(10000)
                    .get()

                // Собираем актуальные заголовки и ссылки с главной страницы ФХМ
                val links = doc.select("a[href]")
                for (el in links) {
                    val text = el.text().trim()
                    val href = el.attr("abs:href")
                    if (text.length > 25 && (href.contains("news") || href.contains("article") || href.contains("match"))) {
                        if (parsedNews.none { it.title == text } && parsedNews.size < 12) {
                            parsedNews.add(NewsItem(title = text, date = "fhmoscow.com", url = href))
                        }
                    }
                }

                if (parsedNews.isEmpty()) {
                    parsedNews.add(
                        NewsItem(
                            title = "Сайт ФХМ загружен (Title: ${doc.title()}). Найдено DOM-элементов: ${doc.allElements.size}",
                            date = "Статус парсера: ОК",
                            url = BASE_URL
                        )
                    )
                }
            } catch (e: Exception) {
                connectionStatus = "Офлайн-режим (${e.localizedMessage ?: "ошибка сети"})"
                parsedNews.add(
                    NewsItem(
                        title = "Не удалось достучаться до fhmoscow.com: $connectionStatus",
                        date = "Проверка связи",
                        url = BASE_URL
                    )
                )
            }

            // Формируем структуру матчей и таблиц для выбранного возраста и группы
            val matches = getSampleMatchesForYear(birthYear, group)
            val standings = getSampleStandingsForYear(birthYear, group)

            Triple(matches, standings, parsedNews)
        }
    }

    private fun getSampleMatchesForYear(year: String, group: String): List<HockeyMatch> {
        return listOf(
            HockeyMatch(
                id = "1",
                homeTeam = "ЦСКА ($year)",
                awayTeam = "Спартак ($year)",
                homeScore = 4,
                awayScore = 3,
                periodScores = "1:1, 2:0, 1:2",
                date = "Сегодня • Первенство Москвы ($group)",
                status = "ОКОНЧЕН",
                arena = "ЛДС ЦСКА",
                protocolEvents = listOf(
                    "04:12 — 1:0 ЦСКА (Иванов А., пас Петров С.)",
                    "14:50 — 1:1 Спартак (Смирнов К., бол.)",
                    "25:10 — 2:1 ЦСКА (Соколов Д.)",
                    "38:02 — 3:1 ЦСКА (Иванов А., пас Морозов И.)",
                    "44:19 — 3:2 Спартак (Волков М.)",
                    "51:40 — 4:2 ЦСКА (Кузнецов Р.)",
                    "58:55 — 4:3 Спартак (Смирнов К., 6x5)"
                )
            ),
            HockeyMatch(
                id = "2",
                homeTeam = "Динамо ($year)",
                awayTeam = "Крылья Советов ($year)",
                homeScore = 2,
                awayScore = 2,
                periodScores = "1:0, 1:2, 0:0",
                date = "Сегодня • Первенство Москвы ($group)",
                status = "3-й ПЕРИОД",
                arena = "ЛД «Арктика»",
                protocolEvents = listOf(
                    "08:30 — 1:0 Динамо (Федоров Н.)",
                    "22:15 — 1:1 Крылья Советов (Орлов В.)",
                    "31:00 — 2:1 Динамо (Алексеев Т., бол.)",
                    "39:45 — 2:2 Крылья Советов (Макаров П.)"
                )
            ),
            HockeyMatch(
                id = "3",
                homeTeam = "Академия Михайлова ($year)",
                awayTeam = "Русь ($year)",
                homeScore = null,
                awayScore = null,
                periodScores = "Превью матча",
                date = "Завтра • Первенство Москвы ($group)",
                status = "13:00",
                arena = "ЛД «Центральный»",
                protocolEvents = listOf("Матч еще не начался. Составы появятся за 60 минут до стартового вбрасывания.")
            ),
            HockeyMatch(
                id = "4",
                homeTeam = "Белые Медведи ($year)",
                awayTeam = "Локомотив-2004 ($year)",
                homeScore = 1,
                awayScore = 5,
                periodScores = "0:2, 1:1, 0:2",
                date = "Вчера • Первенство Москвы ($group)",
                status = "ОКОНЧЕН",
                arena = "ЛД «Белые Медведи»",
                protocolEvents = listOf(
                    "03:11 — 0:1 Локомотив (Громов А.)",
                    "17:20 — 0:2 Локомотив (Белов И.)",
                    "29:05 — 1:2 Белые Медведи (Захаров С.)",
                    "35:44 — 1:3 Локомотив (Громов А.)",
                    "48:12 — 1:4 Локомотив (Титов М.)",
                    "56:01 — 1:5 Локомотив (Козлов Д., ПВ)"
                )
            )
        )
    }

    private fun getSampleStandingsForYear(year: String, group: String): List<StandingRow> {
        return listOf(
            StandingRow(1, "ЦСКА ($year)", 22, 19, 3, "112-41", 38),
            StandingRow(2, "Динамо ($year)", 22, 17, 5, "98-48", 34),
            StandingRow(3, "Спартак ($year)", 22, 16, 6, "94-52", 32),
            StandingRow(4, "Локомотив ($year)", 22, 15, 7, "89-55", 30),
            StandingRow(5, "Академия Михайлова", 22, 12, 10, "76-68", 24),
            StandingRow(6, "Крылья Советов", 22, 10, 12, "65-72", 20),
            StandingRow(7, "Русь ($group)", 22, 8, 14, "54-81", 16),
            StandingRow(8, "Белые Медведи", 22, 5, 17, "43-95", 10)
        )
    }

    fun getSamplePlayers(year: String): List<PlayerStat> {
        return listOf(
            PlayerStat(1, "Иванов Александр", "ЦСКА ($year)", 22, 24, 21, 45),
            PlayerStat(2, "Смирнов Кирилл", "Спартак ($year)", 22, 21, 19, 40),
            PlayerStat(3, "Федоров Никита", "Динамо ($year)", 21, 16, 22, 38),
            PlayerStat(4, "Громов Артем", "Локомотив ($year)", 22, 18, 15, 33),
            PlayerStat(5, "Орлов Владислав", "Крылья Советов", 20, 14, 16, 30),
            PlayerStat(6, "Соколов Дмитрий", "ЦСКА ($year)", 22, 12, 17, 29)
        )
    }
}

// --- ЦВЕТОВАЯ ПАЛИТРА В СТИЛЕ КХЛ ---
val KhlBgDark = Color(0xFF0B101B)
val KhlCardDark = Color(0xFF161F30)
val KhlAccentRed = Color(0xFFE30613)
val KhlIceBlue = Color(0xFF00A8E8)
val KhlTextGray = Color(0xFF8F9BB3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = KhlBgDark,
                    surface = KhlCardDark,
                    primary = KhlAccentRed,
                    secondary = KhlIceBlue
                )
            ) {
                FhmKhlApp()
            }
        }
    }
}

@Composable
fun FhmKhlApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedYear by remember { mutableStateOf("2013 г.р.") }
    var selectedGroup by remember { mutableStateOf("Группа А") }

    val years = listOf("2009 г.р.", "2010 г.р.", "2011 г.р.", "2012 г.р.", "2013 г.р.", "2014 г.р.", "2015 г.р.")
    val groups = listOf("Группа А", "Группа Б", "Группа В")

    var matches by remember { mutableStateOf<List<HockeyMatch>>(emptyList()) }
    var standings by remember { mutableStateOf<List<StandingRow>>(emptyList()) }
    var news by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMatchForProtocol by remember { mutableStateOf<HockeyMatch?>(null) }

    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            isLoading = true
            val (m, s, n) = FhmParser.fetchLiveSiteData(selectedYear, selectedGroup)
            matches = m
            standings = s
            news = n
            isLoading = false
        }
    }

    LaunchedEffect(selectedYear, selectedGroup) {
        loadData()
    }

    Scaffold(
        containerColor = KhlBgDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KhlCardDark)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                // Верхний заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(KhlAccentRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ФХМ • МАТЧ-ЦЕНТР",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = Color.White
                        )
                    }
                }

                // Селектор года рождения
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    years.forEach { year ->
                        val isSelected = year == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) KhlAccentRed else KhlBgDark)
                                .clickable { selectedYear = year }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = year,
                                color = if (isSelected) Color.White else KhlTextGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Селектор группы (А / Б / В)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        val isSelected = group == selectedGroup
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) KhlIceBlue.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedGroup = group }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = group,
                                color = if (isSelected) KhlIceBlue else KhlTextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = KhlCardDark) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.SportsHockey, contentDescription = null) },
                    label = { Text("Матчи") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    label = { Text("Таблица") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    label = { Text("Игроки") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Newspaper, contentDescription = null) },
                    label = { Text("ФХМ Live") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = KhlAccentRed,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (selectedTab) {
                    0 -> MatchesScreen(matches) { match -> selectedMatchForProtocol = match }
                    1 -> StandingsScreen(standings)
                    2 -> PlayersScreen(FhmParser.getSamplePlayers(selectedYear))
                    3 -> NewsParserScreen(news)
                }
            }
        }
    }

    // Модальное окно протокола матча (как в КХЛ)
    selectedMatchForProtocol?.let { match ->
        AlertDialog(
            onDismissRequest = { selectedMatchForProtocol = null },
            containerColor = KhlCardDark,
            title = {
                Column {
                    Text(
                        text = "${match.homeTeam} — ${match.awayTeam}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Счет по периодам: ${match.periodScores} • ${match.arena}",
                        color = KhlIceBlue,
                        fontSize = 12.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ПРОТОКОЛ МАТЧА:",
                        color = KhlTextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    match.protocolEvents.forEach { ev ->
                        Text(
                            text = ev,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMatchForProtocol = null }) {
                    Text("Закрыть", color = KhlIceBlue)
                }
            }
        )
    }
}

@Composable
fun MatchesScreen(matches: List<HockeyMatch>, onMatchClick: (HockeyMatch) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(matches) { match ->
            Card(
                colors = CardDefaults.cardColors(containerColor = KhlCardDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMatchClick(match) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(match.date, color = KhlTextGray, fontSize = 11.sp)
                        val badgeColor = if (match.status.contains("ПЕРИОД")) KhlAccentRed else KhlIceBlue
                        Text(
                            text = match.status,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = match.homeTeam,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(KhlBgDark)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            val scoreText = if (match.homeScore != null) "${match.homeScore} : ${match.awayScore}" else "VS"
                            Text(
                                text = scoreText,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = match.awayTeam,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Периоды: ${match.periodScores}", color = KhlTextGray, fontSize = 12.sp)
                        Text("Протокол ->", color = KhlIceBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun StandingsScreen(standings: List<StandingRow>) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("#", color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.width(28.dp))
                Text("Команда", color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("И", color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("В", color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("П", color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("Ш", color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                Text("О", color = KhlIceBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
            }
        }
        items(standings) { row ->
            Card(
                colors = CardDefaults.cardColors(containerColor = KhlCardDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.rank.toString(),
                        color = if (row.rank <= 3) KhlAccentRed else KhlTextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = row.team,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(row.games.toString(), color = KhlTextGray, fontSize = 13.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text(row.wins.toString(), color = Color.White, fontSize = 13.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text(row.losses.toString(), color = KhlTextGray, fontSize = 13.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text(row.goalsDiff, color = KhlTextGray, fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                    Text(
                        text = row.points.toString(),
                        color = KhlIceBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.width(34.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun PlayersScreen(players: List<PlayerStat>) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "БОМБАРДИРЫ ПЕРВЕНСТВА МОСКВЫ",
                color = KhlTextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(players) { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = KhlCardDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${p.rank}. ${p.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = p.team,
                            color = KhlIceBlue,
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("И", color = KhlTextGray, fontSize = 10.sp)
                            Text("${p.games}", color = Color.White, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Г", color = KhlTextGray, fontSize = 10.sp)
                            Text("${p.goals}", color = Color.White, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("П", color = KhlTextGray, fontSize = 10.sp)
                            Text("${p.assists}", color = Color.White, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ОЧКИ", color = KhlAccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${p.points}", color = KhlIceBlue, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsParserScreen(news: List<NewsItem>) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ПРЯМОЙ ПАРСИНГ С FHMOSCOW.COM (JSOUP):",
                color = KhlIceBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(news) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = KhlCardDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(item.date, color = KhlAccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.url, color = KhlTextGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
