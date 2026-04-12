import pandas as pd
from sqlalchemy import create_engine
from sqlalchemy.types import Text
import os

# Define the file path
file_path = r'D:\sem 6\OOAD\project\OOAD_RESEARCH_COLLABORATION_PROJECT\research-collab\src\main\resources\data\pes_university_staff_data-v2_addded_dept_campus.csv'

print(f"Loading data from: {file_path}")
df = pd.read_csv(file_path)

# Fix phone column (avoid scientific notation issue)
if 'phone' in df.columns:
    df['phone'] = df['phone'].astype(str)

# Connect to MySQL (database: pes_staff_data)
print("Connecting to MySQL database 'pes_staff_data'...")
engine = create_engine('mysql+mysqlconnector://root:1234@localhost/pes_staff_data')

# Define SQLAlchemy types for long text columns to prevent truncation errors
dtypes = {
    'research': Text(),
    'teaching': Text(),
    'about': Text(),
    'publications_journals': Text(),
    'publications_conferences': Text(),
    'education': Text(),
    'responsibilities': Text(),
    'image': Text()
}

# Insert data into the new table
table_name = 'pes_staff_table_2'
print(f"Inserting data into table '{table_name}'...")

df.to_sql(
    name=table_name,
    con=engine,
    if_exists='replace',  # 'replace' creates the table if it doesn't exist
    index=False,
    dtype=dtypes
)

print(f"✅ Data successfully inserted into {table_name}!")
