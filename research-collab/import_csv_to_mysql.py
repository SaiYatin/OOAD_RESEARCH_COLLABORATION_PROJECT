import pandas as pd
from sqlalchemy import create_engine

# STEP 1: Load CSV file
file_path = r'D:\sem 6\OOAD\project\OOAD_RESEARCH_COLLABORATION_PROJECT\prototype - 1\pes_mentor_staff_data_page_1.csv'
df = pd.read_csv(file_path)

# STEP 2: Rename columns (important - match MySQL table)
df.columns = ['name', 'designation', 'mail', 'phone', 'research']

# STEP 3: Fix phone column (avoid scientific notation issue)
df['phone'] = df['phone'].astype(str)

# STEP 4: Connect to MySQL
engine = create_engine('mysql+mysqlconnector://root:1234@localhost/pes_staff_data')

# STEP 5: Insert data into your table
df.to_sql(
    name='pes_staff_table',   # your table name
    con=engine,
    if_exists='append',       # don't delete table
    index=False
)

print("✅ Data inserted successfully!")