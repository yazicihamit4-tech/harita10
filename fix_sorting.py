import re

with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'r') as f:
    content = f.read()

pattern = r"""@Composable
fun AdminEkrani\(\) \{
    var tumSinyaller by remember \{ mutableStateOf<List<Sinyal>>\(emptyList\(\)\) \}
    val coroutineScope = rememberCoroutineScope\(\)
    val context = LocalContext\.current

    fun fetchSinyaller\(\) \{
        coroutineScope\.launch \{
            try \{
                val snapshot = FirebaseFirestore\.getInstance\(\)\.collection\("sinyaller"\)
                    \.orderBy\("timestamp", Query\.Direction\.DESCENDING\)
                    \.get\(\)\.await\(\)
                tumSinyaller = snapshot\.toObjects\(Sinyal::class\.java\)
            \} catch \(e: Exception\) \{
                e\.printStackTrace\(\)
            \}
        \}
    \}

    LaunchedEffect\(Unit\) \{
        fetchSinyaller\(\)
    \}

    Column\(
        modifier = Modifier
            \.fillMaxSize\(\)
            \.padding\(16\.dp\)
            \.verticalScroll\(rememberScrollState\(\)\)
    \) \{
        Text\(
            text = "Admin Paneli - Tüm Sinyaller",
            style = MaterialTheme\.typography\.headlineSmall,
            fontWeight = FontWeight\.Bold,
            modifier = Modifier\.padding\(bottom = 16\.dp\)
        \)"""

replacement = """@Composable
fun AdminEkrani() {
    var tumSinyaller by remember { mutableStateOf<List<Sinyal>>(emptyList()) }
    var isDescending by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun fetchSinyaller() {
        coroutineScope.launch {
            try {
                val direction = if (isDescending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
                val snapshot = FirebaseFirestore.getInstance().collection("sinyaller")
                    .orderBy("timestamp", direction)
                    .get().await()
                tumSinyaller = snapshot.toObjects(Sinyal::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isDescending) {
        fetchSinyaller()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tüm Sinyaller",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { isDescending = !isDescending }) {
                Text(if (isDescending) "↓ Yeniden Eskiye" else "↑ Eskiden Yeniye")
            }
        }"""

content = re.sub(pattern, replacement, content)

with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'w') as f:
    f.write(content)

print("Sorting UI Re-added")
