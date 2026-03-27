import re

with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'r') as f:
    content = f.read()

pattern = r"""                                        FirebaseFirestore.getInstance\(\)\.collection\("sinyaller"\)
                                            \.document\(yeniSinyal\.id\)
                                            \.set\(yeniSinyal\)\.await\(\)"""

replacement = """                                        FirebaseFirestore.getInstance().collection("sinyaller")
                                            .document(yeniSinyal.id)
                                            .set(yeniSinyal).await()

                                        NotificationSender.sendNotificationToAdmins(
                                            context = context,
                                            isim = isimSoyisim,
                                            aciklama = yorum
                                        )"""

content = re.sub(pattern, replacement, content)

with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'w') as f:
    f.write(content)

print("Admin Notification Fixed")
