with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("kotlinx.coroutines.tasks.await(FirebaseMessaging.getInstance().token)", "FirebaseMessaging.getInstance().token.await()")

with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'w') as f:
    f.write(content)

print("Fixed fcmToken await syntax")
