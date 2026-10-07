import shutil

# Make backup of AdminDashboardScreen.kt
shutil.copyfile(
    r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt',
    r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt.bak'
)
print("Backup created successfully.")
