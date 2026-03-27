import re

with open('src/main/java/com/yazhamit/izmirharita/MainActivity.kt', 'r') as f:
    content = f.read()

# Check brackets
left_brackets = content.count('{')
right_brackets = content.count('}')
print(f"Brackets: {{ = {left_brackets}, }} = {right_brackets}")
