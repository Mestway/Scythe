# 1
### [select the rows with max value on a column](http://stackoverflow.com/questions/7745609/sql-select-only-rows-with-max-value-on-a-column)
#### description

    How do I select one row per id and only the greatest rev?

#### input

    | id   |  rev   |  content  |
    |---------------------------|
    | 1    |  1     |  A        |
    | 2    |  1     |  B        |
    | 1    |  2     |  C        |
    | 1    |  3     |  D        |


#### output

    | col1 | col2 | col3 |
    |--------------------|
    |  1   |  3   |  D   |
    |  2   |  1   |  B   |

# 2
### [SQL query to get the “latest” value for each location](http://stackoverflow.com/questions/1783932/sql-query-to-get-the-latest-value-for-each-location)
#### description
    I am trying to get the latest temp for each locId that has updated values within the last 20 min.

    So the result I want from the table above would be (say I run the query at 2009-02-25 10:10):

#### input


    | locId  |    dtg                |  temp |
    |----------------------------------------|
    | 100    |  2009-02-25 10:00:00  |  15   |
    | 200    |  2009-02-25 10:00:00  |  20   |
    | 300    |  2009-02-25 10:00:00  |  24   |
    | 100    |  2009-02-25 09:45:00  |  13   |
    | 300    |  2009-02-25 09:45:00  |  16   |
    | 200    |  2009-02-25 09:45:00  |  18   |
    | 400    |  2009-02-25 09:45:00  |  12   |
    | 100    |  2009-02-25 09:30:00  |  11   |
    | 300    |  2009-02-25 09:30:00  |  14   |
    | 200    |  2009-02-25 09:30:00  |  15   |
    | 400    |  2009-02-25 09:30:00  |  10   |


#### output


    | locID |    dtg                  | tmp |
    |---------------------------------------|
    | 100   |   2009-02-25 10:00:00   | 15  |
    | 200   |   2009-02-25 10:00:00   | 20  |
    | 300   |   2009-02-25 10:00:00   | 24  |


# 3
### [MySQL: Select N rows, but with only unique values in one column](http://stackoverflow.com/questions/190702/mysql-select-n-rows-but-with-only-unique-values-in-one-column)
#### description
    I need to find the 3 oldest persons, but only one of every city.


#### input

  | ID  | Name            | City           | Birthyear  |
  |-----------------------------------------------------|
  | 1   | Egon Spengler   | New York       | 1957       |
  | 2   | Mac Taylor      | New York       | 1955       |
  | 3   | Sarah Connor    | Los Angeles    | 1959       |
  | 4   | Jean-Luc Picard | La Barre       | 2305       |
  | 5   | Ellen Ripley    | Nostromo       | 2092       |
  | 6   | James T. Kirk   | Riverside      | 2233       |
  | 7   | Henry Jones     | Chicago        | 1899       |

#### output

  | col1          |  col2       |
  |-----------------------------|
  | Henry Jones   | Chicago     |
  | Mac Taylor    | New York    |
  | Sarah Connor  | Los Angeles |
   

# 4
### [Select first row in each GROUP BY group?](http://stackoverflow.com/questions/3800551/select-first-row-in-each-group-by-group)
#### description

    I'd like to query for the id of the largest purchase (total) made by each customer. Something like this:

    SELECT FIRST(id), customer, FIRST(total)
    FROM  purchases
    GROUP BY customer
    ORDER BY total DESC;


#### input

    | id  | customer | total |
    |------------------------|
    | 1   | Joe      | 5     |
    | 2   | Sally    | 3     |
    | 3   | Joe      | 2     |
    | 4   | Sally    | 1     |


#### output


    | FIRST(id) | customer | FIRST(total) |
    |-------------------------------------|
    |         1 | Joe      | 5            |
    |         2 | Sally    | 3            |


# 5
### [Retrieving the last record in each group](http://stackoverflow.com/questions/1313120/retrieving-the-last-record-in-each-group)
#### description
	 That is, the last record in each group should be returned.


#### input

    | Id |  Name |  Other_Columns |
    |-----------------------------|
    | 1  |  A    |   A_data_1     |
    | 2  |  A    |   A_data_2     |
    | 3  |  A    |   A_data_3     |
    | 4  |  B    |   B_data_1     |
    | 5  |  B    |   B_data_2     |
    | 6  |  C    |   C_data_1     |

#### output

    | col1 | col2 | col3     |
    |------------------------|
    | 3    | A    | A_data_3 |
    | 5    | B    | B_data_2 |
    | 6    | C    | C_data_1 |


# 6
### [SQL query to compare product sales by month](http://stackoverflow.com/questions/17194/sql-query-to-compare-product-sales-by-month?rq=1)
#### description

    The view has a product category, a revenue, a year and a month. I want to create a report comparing 2007 and 2008, showing 0 for the months with no sales. So the report should look something like this:

#### input


    | Category | Revenue  |   Yearh  |  Month |
    |-----------------------------------------|
    | Bikes    |  10 000  |    2008  |      1 |
    | Bikes    |  12 000  |    2008  |      2 |
    | Bikes    |  12 000  |    2008  |      3 |
    | Bikes    |  15 000  |    2008  |      1 |
    | Bikes    |  11 000  |    2007  |      2 |
    | Bikes    |  11 500  |    2007  |      3 |
    | Bikes    |  15 400  |    2007  |      4 |


#### output


    | Category  |  Month  |  Rev. This Year  |  Rev. Last Year |
    |----------------------------------------------------------|
    | Bikes     |     1   |       10 000     |          0      |
    | Bikes     |     2   |       12 000     |          11 000 |
    | Bikes     |     3   |       12 000     |          11 500 |
    | Bikes     |     4   |       0          |          15 400 |

    The key thing to notice is how month 1 only has sales in 2008, and therefore is 0 for 2007. Also, month 4 only has no sales in 2008, hence the 0, while it has sales in 2007 and still show up.

    Also, the report is actually for financial year - so I would love to have empty columns with 0 in both if there was no sales in say month 5 for either 2007 or 2008.


# 7
### [Selecting quarters within a date range](http://stackoverflow.com/questions/14995024/selecting-quarters-within-a-date-range?rq=1)

#### description
    The query I am having trouble with is getting a yrq where the start and end dates are completely within it. Example: I want to find the yrq for the date range 2013-02-01 to 2013-02-15, which would be B233.

#### input

    | yrq  | start_date | end_date   |
    |--------------------------------|
    | B233 | 2013-01-07 | 2013-03-23 |
    | B232 | 2012-09-24 | 2012-12-20 |
    | B231 | 2012-06-25 | 2012-09-13 |
    | B124 | 2012-04-02 | 2012-06-21 |
    | B123 | 2012-01-09 | 2012-03-27 |


#### output

    | col1 |
    |------|
    | B233 |


# 8
### [sqlite3 query by max and filter by second factor](http://stackoverflow.com/questions/32779941/sqlite3-query-by-max-and-filter-by-second-factor)
#### description

    1. SELECT * WHERE from_user <> id
    2. GROUP BY conversation_id
    3. SELECT in every group row with MAX(timestamp) **(if there are two same timestamps in a group use second factor as highest message_id)** !!!
    4. then results SORT BY timestamp


#### input

    | message_id | conversation_id | from_user | timestamp  |  message    |
    |-----------------------------------|
    | 2  | 145  | xxx | 10000 | message |
    | 6  | 1743 | yyy | 999   | message |
    | 7  | 14   | bbb | 899   | message |
    | 1  | 145  | xxx | 10000 | message |
    | 5  | 1743 | me  | 1200  | message |


#### output

    | c1 | c2   |  c3 | c4    |  c5     |
    |-----------------------------------|
    | 2  | 145  | xxx | 10000 | message |
    | 6  | 1743 | yyy | 999   | message |
    | 7  | 14   | bbb | 899   | message |


# 9
### [How to group following rows by not unique value](http://stackoverflow.com/questions/30877926/how-to-group-following-rows-by-not-unique-value?rq=1)
#### description

    I would like to know in which time interval I was on which way.
    Is there a possibility to do the partition according to the order, means grouping only following attributes, that are equal?

#### input

    @table1

    | id | way | time  |
    |------------------|
    | 1  | 1   | 00:01 |
    | 2  | 1   | 00:02 |
    | 3  | 2   | 00:03 |
    | 4  | 2   | 00:04 |
    | 5  | 2   | 00:05 |
    | 6  | 3   | 00:06 |
    | 7  | 3   | 00:07 |
    | 8  | 1   | 00:08 |
    | 9  | 1   | 00:09 |


#### output


    | id | way | from  | to    |
    |--------------------------|
    | 1  | 1   | 00:01 | 00:02 |
    | 3  | 2   | 00:03 | 00:05 |
    | 6  | 3   | 00:06 | 00:07 |
    | 8  | 1   | 00:08 | 00:09 |


# 10
### [SELECT / GROUP BY - segments of time (10 seconds, 30 seconds, etc)](http://stackoverflow.com/questions/3086386/select-group-by-segments-of-time-10-seconds-30-seconds-etc)
#### description

    What I would like to do, is get sums and averages of the count column over a range of times. For instance, I have samples every 2 seconds recorded, but I would like the sum of the count column for all the samples in a 10 second or 30 second window for all samples.

    # This is the sum for the 00 - 30 seconds range
    # This is the sum for the 30 - 60 seconds range
    # This is the sum for the 30 - 60 seconds range

#### input


     | time_stamp          | count           |
     |---------------------------------------|
     | 2010-06-15 23:35:28 |               1 |
     | 2010-06-15 23:35:30 |               1 |
     | 2010-06-15 23:35:30 |               1 |
     | 2010-06-15 23:35:30 |             942 |
     | 2010-06-15 23:35:30 |             180 |
     | 2010-06-15 23:35:30 |               4 |
     | 2010-06-15 23:35:30 |              52 |
     | 2010-06-15 23:35:30 |              12 |
     | 2010-06-15 23:35:30 |               1 |
     | 2010-06-15 23:35:30 |               1 |
     | 2010-06-15 23:35:33 |            1468 |
     | 2010-06-15 23:35:33 |             247 |
     | 2010-06-15 23:35:33 |               1 |
     | 2010-06-15 23:35:33 |              81 |
     | 2010-06-15 23:35:33 |              16 |
     | 2010-06-15 23:35:35 |            1828 |
     | 2010-06-15 23:35:35 |             214 |
     | 2010-06-15 23:35:35 |              75 |
     | 2010-06-15 23:35:35 |               8 |
     | 2010-06-15 23:35:37 |            1799 |
     | 2010-06-15 23:35:37 |              24 |
     | 2010-06-15 23:35:37 |              11 |
     | 2010-06-15 23:35:37 |               2 |
     | 2010-06-15 23:35:40 |             575 |
     | 2010-06-15 23:35:40 |               1 |
     | 2010-06-17 10:39:35 |               2 |
     | 2010-06-17 10:39:35 |               2 |
     | 2010-06-17 10:39:35 |               1 |
     | 2010-06-17 10:39:35 |               2 |
     | 2010-06-17 10:39:35 |               1 |
     | 2010-06-17 10:39:40 |              35 |
     | 2010-06-17 10:39:40 |              19 |
     | 2010-06-17 10:39:40 |              37 |
     | 2010-06-17 10:39:42 |              64 |
     | 2010-06-17 10:39:42 |               3 |
     | 2010-06-17 10:39:42 |              31 |
     | 2010-06-17 10:39:42 |               7 |
     | 2010-06-17 10:39:42 |             246 |


#### output

     |          c1         |  c2  |
     |----------------------------|
     | 2010-06-15 23:35:00 |    1 |  
     | 2010-06-15 23:35:30 | 7544 |  
     | 2010-06-17 10:39:35 |  450 |  


# 11
### [How can I SELECT rows with MAX(Column value), DISTINCT by another column in SQL?](http://stackoverflow.com/questions/612231/how-can-i-select-rows-with-maxcolumn-value-distinct-by-another-column-in-sql?rq=1)
#### description
    I need to select each distinct home holding the maximum value of datetime.


#### input


    | id | home|  datetime  | player  | resource |
    |--------------------------------------------|
    | 1  | 10  | 04/03/2009 | john    | 399      |
    | 2  | 11  | 04/03/2009 | juliet  | 244      |
    | 5  | 12  | 04/03/2009 | borat   | 555      |
    | 3  | 10  | 03/03/2009 | john    | 300      |
    | 4  | 11  | 03/03/2009 | juliet  | 200      |
    | 6  | 12  | 03/03/2009 | borat   | 500      |
    | 7  | 13  | 24/12/2008 | borat   | 600      |
    | 8  | 13  | 01/01/2009 | borat   | 700      |


#### output


    | id | home|  datetime  | player | resource |
    |-------------------------------------------|
    | 1  | 10  | 04/03/2009 | john   | 399      |
    | 2  | 11  | 04/03/2009 | juliet | 244      |
    | 5  | 12  | 04/03/2009 | borat  | 555      |
    | 8  | 13  | 01/01/2009 | borat  | 700      |


# 12
### [SQL Select only rows with custom Max Value on a Column](http://stackoverflow.com/questions/20491912/sql-select-only-rows-with-custom-max-value-on-a-column?rq=1)
#### description
    regarding a MySQL request to select only rows with Max Value on a Column.
    it has been determined that rev=2 has a higher priority than both rev=3 and rev=1.


#### input


    |  id |  rev |  content |
    |-----------------------|
    |  1  |   1  |   A      |
    |  2  |   1  |   B      |
    |  1  |   2  |   C      |
    |  1  |   3  |   D      |


#### output

    | c1 | c2 | c3 |
    |--------------|
    | 1  | 2  | C  |
    | 2  | 1  | B  |


# 13
### [SQL max() only returns 1 row if column has several max values](http://stackoverflow.com/questions/33027549/sql-max-only-returns-1-row-if-column-has-several-max-values)
#### description
    What I need is to get the two rows with the maximum value (I don't know the values in advance) which means that my example above shoud give:


#### input


    |  a  | b |
    |---------|
    | ALA | 2 |
    | ASP | 1 |
    | SER | 1 |
    | VAL | 2 |


#### output


    |  a  | max(b) |
    |--------------|
    | ALA |   2    |
    | VAL |   2    |


# 14
### [Finding duplicate values in a SQL table](http://stackoverflow.com/questions/2594829/finding-duplicate-values-in-a-sql-table)
#### description
    I want is to get duplicates with the same email and name.


#### input


    |  ID |  NAME |  EMAIL       |
    |----------------------------|
    |  1  |  John |  asd@asd.com |
    |  2  |  Sam  |  asd@asd.com |
    |  3  |  Tom  |  asd@asd.com |
    |  4  |  Bob  |  bob@asd.com |
    |  5  |  Tom  |  asd@asd.com |


#### output

    |  c  |
    |-----|
    | Tom |
    | Tom |


# 15
### [SQL moving average](http://stackoverflow.com/questions/10624902/sql-moving-average)
#### description
	 How do you create a moving average in SQL?


#### input


    | Date         |   Clicks |
    |-------------------------|
    | 2012-05-01   |    2230  |
    | 2012-05-02   |    3150  |
    | 2012-05-03   |    5520  |
    | 2012-05-04   |    1330  |
    | 2012-05-05   |    2260  |
    | 2012-05-06   |    3540  |
    | 2012-05-07   |    2330  |

#### output


    |  Date        |    Clicks  | 3 day Moving Average |
    |--------------------------------------------------|
    |  2012-05-01  |     2230   |                      |
    |  2012-05-02  |     3150   |                      |
    |  2012-05-03  |     5520   |       4360           |
    |  2012-05-04  |     1330   |       3330           |
    |  2012-05-05  |     2260   |       3120           |
    |  2012-05-06  |     3540   |       3320           |
    |  2012-05-07  |     2330   |       3010           |


# 16
### [How to compute proportion of a particular field in SQL?](http://stackoverflow.com/questions/33046398/how-to-compute-proportion-of-a-particular-field-in-sql)
#### description
    in a single query, given a particular ID, I want to compute the proportion of entries that has action Foo. For example, given A, there are 2 Foos out of 3 actions. So it'd be 66%.


#### input

    | ID  |     Date      | Action   | Params|
    |----------------------------------------|
    |  A  |  2015-10-01   |   Foo    |     1 |
    |  A  |  2015-10-02   |   Foo    |     2 |
    |  A  |  2015-10-01   |   Bar    |    10 |
    |  B  |  2015-10-01   |   Foo    |     0 |
    |  B  |  2015-10-02   |   Foo    |     0 |
    |  B  |  2015-10-03   |   Bar    |     1 |


#### output


    | c1 | c2  |
    |----------|
    | A  | 66% |
    | B  | 66% |


# 17
### [SQL Query for 7 Day Rolling Average in SQL Server](http://stackoverflow.com/questions/25922379/sql-query-for-7-day-rolling-average-in-sql-server)
#### description
    I need to generate a report that gives daily usage and last 7 days’ rolling average


#### input


    | productid | date        | hour | usagecount |
    |---------------------------------------------|
    |  1        | 2014-09-01  | 0    | 10         |
    |  1        | 2014-09-01  | 1    | 15         |
    |  1        | 2014-09-02  | 5    | 25         |
    |  1        | 2014-09-03  | 5    | 25         |
    |  1        | 2014-09-04  | 3    | 25         |
    |  1        | 2014-09-05  | 7    | 25         |
    |  1        | 2014-09-06  | 10   | 25         |
    |  1        | 2014-09-07  | 9    | 25         |
    |  1        | 2014-09-08  | 5    | 25         |
    |  2        | 2014-09-03  | 16   | 10         |
    |  2        | 2014-09-03  | 13   | 115        |


#### output

    | productid	| date	        | dailyusage	| rolling_avg |
    |-----------------------------------------------------|
    | 1	        | 2014-09-01	| 25	        | 25          |
    | 1	        | 2014-09-02	| 25	        | 25          |
    | 1	        | 2014-09-03	| 25	        | 25          |
    | 1	        | 2014-09-04	| 25	        | 25          |
    | 1	        | 2014-09-05	| 25	        | 25          |
    | 1	        | 2014-09-06	| 25	        | 25          |
    | 1	        | 2014-09-07	| 25	        | 25          |
    | 1	        | 2014-09-08	| 25	        | 25          |
    | 2	        | 2014-09-03	| 125	        | 125         |


# 18
### [SQL exists comparison](http://stackoverflow.com/questions/11898003/sql-exists-comparison)
#### description

    I need to select rows that have one treated row for one country and there exists one untreated row for the same country. For below table I would then return row with id = 1.

    So first I need to check if there is a row with TREATED status, then I have to check if there is a row with UNTREATED status and the same country as the one with the TREATED status.


#### input


      @Table example 1

      | id | country| status       |
      |----------------------------|
      | 1  | SE     | TREATED      |
      | 2  | DK     | UNTREATED    |
      | 3  | SE     | UNTREATED    |


#### output


      @Result of query

      | id | country| status       |
      |----------------------------|
      | 1  | SE     | TREATED      |


# 19
### [how do I query sql for a latest record date for each user](http://stackoverflow.com/questions/2411559/how-do-i-query-sql-for-a-latest-record-date-for-each-user)
#### description


    How do I create a query that would give me the latest date for each user?

    Update: I forgot that I needed to have a value that goes along with the latest date.


#### input


    | username |   date     | value|
    |------------------------------|
    | brad     | 2010-01-02 |  1.1 |
    | fred     | 2010-01-03 |  1.0 |
    | bob      | 2009-08-04 |  1.5 |
    | brad     | 2010-02-02 |  1.2 |
    | fred     | 2009-12-02 |  1.3 |


#### output

    | username |   date      | value|
    |-------------------------------|
    | fred     | 2010-01-03  |  1.0 |
    | bob      | 2009-08-04  |  1.5 |
    | brad     | 2010-02-02  |  1.2 |


# 20
### [How to create a SQL Server function to “join” multiple rows from a subquery into a single delimited field?](http://stackoverflow.com/questions/6899/how-to-create-a-sql-server-function-to-join-multiple-rows-from-a-subquery-into)
#### description

    How to create a SQL Server function to “join” multiple rows from a subquery into a single delimited field?

#### input


    @table1

    | VehicleID | Name |
    |------------------|
    | 1         | Chuck|
    | 2         | Larry|

    @table2

    | LocationID | VehicleID |  City        |
    |---------------------------------------|
    | 1          | 1         |  New York    |
    | 2          | 1         |  Seattle     |
    | 3          | 1         |  Vancouver   |
    | 4          | 2         |  Los Angeles |
    | 5          | 2         |  Houston     |


#### output


    | VehicleID  | Name   | Locations                     |
    |-----------------------------------------------------|
    | 1          | Chuck  | New York, Seattle, Vancouver  |
    | 2          | Larry  | Los Angeles, Houston          |


# 21
### [Count distinct value pairs in multiple columns in SQL](http://stackoverflow.com/questions/18715798/count-distinct-value-pairs-in-multiple-columns-in-sql)
#### description
    I want a query that count the number of rows, but distinct by 3 parameters.


#### input


    | Id   |   Name       |  Adress   |
    |---------------------------------|
    | 1    | MyName       |  MyAdress |
    | 2    | MySecondName |  Adress2  |


#### output


    | c |
    |---|
    | 2 |


# 22
### [SQL:Count of values in one column in relation to another column](http://stackoverflow.com/questions/31165814/sqlcount-of-values-in-one-column-in-relation-to-another-column)
#### description

    I need the count of deviceid based on the number of timestamps it is seen in. So the output would be something like: 0add is seen in 2 timestamps hence the count is 2 whereas 0bdd is seen in one time stamp hence 0bdd has count of 1. The number of licenses corresponding to the device per time stamp is not considered for the count.


#### input


    |  id   |    date     | time_stamp | licenseid |  storeid | deviceid |value|
    |--------------------------------------------------------------------------|
    |  1    |  2015-06-12 |  17:36:15  | lic0001   |    1     |  0add    |  52 |
    |  2    |  2015-06-12 |  17:36:15  | lic0002   |    1     |  0add    |  54 |
    |  3    |  2015-06-12 |  17:36:15  | lic0003   |    1     |  0add    |  53 |
    |  4    |  2015-06-12 |  17:36:21  | lic0001   |    1     |  0add    |  54 |
    |  5    |  2015-06-12 |  17:36:21  | lic0002   |    1     |  0add    |  59 |
    |  6    |  2015-06-12 |  17:36:21  | lic0003   |    1     |  0add    |  62 |
    |  7    |  2015-06-12 |  17:36:21  | lic0004   |    1     |  0add    |  55 |
    |  8    |  2015-06-12 |  17:36:15  | lic0001   |    1     |  0bdd    |  53 |
    |  9    |  2015-06-12 |  17:36:15  | lic0002   |    1     |  0bdd    |  52 |
    |  10   |  2015-06-12 |  17:36:15  | lic0003   |    1     |  0bdd    |  52 |



#### output


    | date        | deviceid | count |
    |--------------------------------|
    | 2015-06-12  | 0add     | 2     |
    | 2015-06-12  | 0bdd     | 1     |


# 23
### [SQL select counts on 1 value](http://stackoverflow.com/questions/31848698/sql-select-counts-on-1-value)
#### description

    I want to count how many of each type that has 'Error' as status

#### input


    |   Id    |    Name    | Status | Content_type |
    |----------------------------------------------|
    | 2960671 | PostJob    | Error  | general_url  |
    | 2960670 | auto_index | Done   | general_url  |
    | 2960669 | auto_index | Done   | document     |
    | 2960668 | auto_index | Error  | document     |
    | 2960667 | auto_index | Error  | document     |


#### output

    | c1  | c2           |
    |--------------------|
    | 1   |  general_url |
    | 2   |   document   |


# 24
### [How to return the row with the highest sum of two columns?](http://stackoverflow.com/questions/32773790/how-to-return-the-row-with-the-highest-sum-of-two-columns)
#### description

  I'm looking for the SQL query that returns the name of the player with the highest GoalsScored + GoalsSaved, as long is that player is not listed in the table of taken players.


#### input


    @table1

    |     Name      | GoalsScored  | GoalsSaved  |
    |--------------------------------------------|
    | John Smith    |           15 |          12 |
    | John Doe      |           12 |          20 |
    | Bob John      |            7 |           6 |
    | John Bob      |           10 |          30 |
    | Bobby Johnson |           25 |          30 |

    @taken_players
    
        And a temporary table to store the names of already chosen players (taken_players), which looks something like this:
    
    | takenplayername |
    |-----------------|
    | Bob John        |
    | Bobby Johnson   |


#### output

    | name     |
    |----------|
    | John Bob |


# 25 **
### [Only joining rows where the date is less than the max date in another field](http://stackoverflow.com/questions/31941909/only-joining-rows-where-the-date-is-less-than-the-max-date-in-another-field)
#### description

    I want to join the two tables so that I only include sales dates that are less than the maximum promotion date. So the result would look something like this


#### input

    Let's say I have two tables. One table containing employee information and the days that employee was given a promotion:

    @table1
    
    |  Emp_ID  |   Promo_Date |
    |-------------------------|
    |  1       |   07/01/2012 |
    |  1       |   07/01/2013 |
    |  2       |   07/19/2012 |
    |  2       |   07/19/2013 |
    |  3       |   08/21/2012 |
    |  3       |   08/21/2013 |

    And another table with every day employees closed a sale:

    @table2

    | Emp_ID   |   Sale_Date  |
    |-------------------------|
    | 1        |   06/12/2013 |
    | 1        |   06/30/2013 |
    | 1        |   07/15/2013 |
    | 2        |   06/15/2013 |
    | 2        |   06/17/2013 |
    | 2        |   08/01/2013 |
    | 3        |   07/31/2013 |
    | 3        |   09/01/2013 |


#### output


    | Emp_ID  |   Sale_Date  | Promo_Date |
    |-------------------------------------|
    | 1       |   06/12/2013 | 07/01/2012 |
    | 1       |   06/30/2013 | 07/01/2012 |
    | 1       |   06/12/2013 | 07/01/2013 |
    | 1       |   06/30/2013 | 07/01/2013 |


# 26
### [Select records with certain value but exclude if it has another one](http://stackoverflow.com/questions/32045677/select-records-with-certain-value-but-exclude-if-it-has-another-one)
#### description

    Basically I need to select the product_ID's that's exclusive to client_id = 2.

#### input


    | Product_ID | Client_ID  |
    |-------------------------|
    | 1          | 2          |
    | 1          | 3          |
    | 2          | 2          |
    | 3          | 2          |


#### output

    | c1 |
    |----|
    | 2  |
    | 3  |


# 27
### [Joining to two tables and then using sum() and count()](http://stackoverflow.com/questions/33069068/sql-joining-to-two-tables-and-then-using-sum-and-count)
#### description

  How could I get the output the number of staff that work in school and the salary of each school.


#### input


    @table1

    | school_id | class_id | school_location |
    |----------------------------------------|
    | 400       |      50  |     Arizona     |

    @staff

    | staff_id | forename | school_id | wage |
    |----------------------------------------|
    |   1      |  Peter   |    400    | 5000 |


#### output


    | school_id | numberofstaff | salary|
    |-----------------------------------|
    | 400       |     1         | 5000  |
    

# 29
### [Foreign key in same table, unable to get right query](http://stackoverflow.com/questions/33043326/foreign-key-in-same-table-unable-to-get-right-query)
#### description

    ID is the primary key and Parent is a foreign key referencing ID What I'm trying to do is to list every parents ID and name.

#### input


    | ID   |  Name   |   Age   |  Parent  |
    |-------------------------------------|
    | 1    |  Bob    |    50   |   NULL   |
    | 2    |  Matt   |    20   |     1    |
    | 3    |  Rick   |    18   |     1    |



#### output

    | ID |   Name |
    |-------------|
    | 1  |   Bob  |


# 30
### [Get the result based on column attribute value](http://stackoverflow.com/questions/33037505/get-the-result-based-on-column-attribute-value)
#### description

    I want to only get those Plan (with all their attributes) which have their IsActive attribute's value is set to True.

#### input


    |  Id  |  Plan  | Attributes  | Value  |
    |--------------------------------------|
    |  1   |   A    |   Name      |  AAA   |
    |  2   |   A    |   Class     |  P     |
    |  3   |   A    |   IsActive  |  True  |
    |  4   |   B    |   Name      |  BBB   |
    |  5   |   B    |   Class     |  Q     |
    |  6   |   B    |   IsActive  |  False |
    |  7   |   C    |   Name      |  CCC   |
    |  8   |   C    |   Class     |  R     |
    |  9   |   C    |   IsActive  |  True  |


#### output


    |  Id |  Plan | Attributes  | Value  |
    |------------------------------------|
    |  1  |   A   |   Name      |  AAA   |
    |  2  |   A   |   Class     |  P     |
    |  3  |   A   |   IsActive  |  True  |
    |  7  |   C   |   Name      |  CCC   |
    |  8  |   C   |   Class     |  R     |
    |  9  |   C   |   IsActive  |  True  |


# 31
### [sql group by sub group](http://stackoverflow.com/questions/29430408/sql-group-by-sub-group)
#### description

    I've tried to find a pure sql solution so that i can see: in each city, which colour of car has the largest quantity.


#### input


    |   city      |  car_colour |  car_type |  qty     |
    |--------------------------------------------------|
    | manchester  |  Red        |  Sports   |  7       |
    | manchester  |  Red        |  4x4      |  9       |
    | manchester  |  Blue       |  4x4      |  8       |
    | london      |  Red        |  Sports   |  2       |
    | london      |  Blue       |  4x4      |  3       |
    | leeds       |  Red        |  Sports   |  5       |
    | leeds       |  Blue       |  Sports   |  6       |
    | leeds       |  Blue       |  4X4      |  1       |


#### output


    | c1         | c2   |
    |-------------------|
    | manchester | red  |
    | london     | blue |
    | leeds      | blue |


# 32
### [How to select where multiple rows have the same values in two columns, respectively?](http://stackoverflow.com/questions/30575622/how-to-select-where-multiple-rows-have-the-same-values-in-two-columns-respectiv)
#### description

    they have multiple players with the same name.

#### input


    |   Team   |    Player |
    |----------------------|
    |    1     |    John   |
    |    1     |    Billy  |
    |    2     |    Dillan |
    |    2     |    Brady  |
    |    2     |    Brady  |
    |    3     |    John   |
    |    4     |    Gary   |
    |    4     |    Gary   |


#### output


    | c1 |
    |----|
    | 2  |
    | 4  |


# 33
### [Select rows with same id but different value in another column](http://stackoverflow.com/questions/21162812/select-rows-with-same-id-but-different-value-in-another-column)
#### description


  What i would like to select is the ARIDNR that occurs more than on times. With the different LIEFNR.


#### input


    |ARIDNR|LIEFNR|
    |-------------|
    |1     |A     |
    |2     |A     |
    |3     |A     |
    |1     |B     |
    |2     |B     |


#### output


    |ARIDNR|LIEFNR|
    |-------------|
    |1     |A     |
    |1     |B     |
    |2     |A     |
    |2     |B     |


# 34
### [Several conditions on the same table](http://stackoverflow.com/questions/30265358/several-conditions-on-the-same-table)
#### description

    I try to figure out the people who went to several locations.


#### input


    | Customer    | email          |   ZIP | shop |
    |---------------------------------------------|
    | John Smith  | js@mail.com    | 75016 |    1 |
    | Mary King   | mary@ymail.com | 97430 |    2 |
    | John Smith  | js@mail.com    | 75016 |    3 |
    | Ivan Turtle | ivan@mail.com  | 56266 |    5 |
    | Mary King   | mary@ymail.com | 97430 |    5 |
    | John Smith  | js@mail.com    | 75016 |    5 |


#### output

    | c1         |
    |------------|
    | John Smith |
    | Mary King  |


# 35
### [Matching multiple attributes](http://stackoverflow.com/questions/29442201/matching-multiple-attributes)
#### description

    I want to know which stores sell the same fruit and what that fruit is.


#### input

    |  ID |  Fruit    |
    |-----------------|
    |  1  |  apples   |
    |  1  |  bananas  |
    |  2  |  apples   |
    |  2  |  oranges  |
    |  2  |  cherries |
    |  2  |  lychees  |
    |  3  |  bananas  |
    |  3  |  cherries |
    |  3  |  lychees  |


#### output


    |  ID1 | ID2 | Fruit    |
    |-----------------------|
    |  1   | 2   | apples   |
    |  1   | 3   | bananas  |
    |  2   | 3   | cherries |
    |  2   | 3   | lychees  |


# 36
### [Get distinct values in a table](http://stackoverflow.com/questions/30064469/get-distinct-values-in-a-table)
#### description

    I want to sum up the value field based on the ErrorName and return something like this:


#### input


    |  ErrorName | Value |
    |--------------------|
    |  Error1    | 3     |
    |  Error2    | 2     |
    |  Error3    | 2     |
    |  Error1    | 1     |
    |  Error2    | 1     |


#### output

    | c1      | c2 |
    |--------------|
    | Error1  | 4  |
    | Error2  | 3  |
    | Error3  | 2  |


# 37
### [SQL query to find all names where same user_id count > 3](http://stackoverflow.com/questions/27103777/sql-query-to-find-all-names-where-same-user-id-count-3)
#### description
	 I have following table and I want to display all names along with the user_id where unique user_id count>3.


#### input


    | user_id |  names |
    |------------------|
    | 701     |  Name1 |
    | 701     |  Name2 |
    | 701     |  Name3 |
    | 701     |  Name4 |
    | 702     |  Name5 |
    | 702     |  Name6 |
    | 703     |  Name7 |
    | 703     |  Name8 |


#### output


    |  user_id  |   names |
    |---------------------|
    |  701      |   Name1 |
    |  701      |   Name2 |
    |  701      |   Name3 |
    |  701      |   Name4 |


# 38
### [How to display latest inserted records?](http://stackoverflow.com/questions/30234427/how-to-display-latest-inserted-records)
#### description

    I want to display the latest inserted records in gridview.


#### input


    |  Id | Alerts  |  Alert_Date          |
    |--------------------------------------|
    |  1  | Alert1  |  05/11/2015 12:12:22 |
    |  2  | Alert2  |  05/11/2015 12:12:22 |
    |  3  | Alert1  |  05/12/2015 12:12:22 |
    |  4  | Alert2  |  05/13/2015 12:12:22 |
    |  5  | Alert2  |  05/14/2015 12:12:22 |
    |  6  | Alert3  |  05/14/2015 12:12:22 |


#### output


    | Alerts | Alert_Date          |
    |------------------------------|
    | Alert1 | 05/12/2015 12:12:22 |
    | Alert2 | 05/14/2015 12:12:22 |
    | Alert3 | 05/14/2015 12:12:22 |


# 39
### [combining dates when doing a sum query](http://stackoverflow.com/questions/33134803/combining-dates-when-doing-a-sum-query)
#### description

    The idea is to sum the observation by Object and Date.
    Ideally what I would like to do though is to make it so that rather than getting a numeric month, mm, I get a date mm/dd/yyyy.


#### input


     | Object | Observation | Date       |
     |-----------------------------------|
     | 1      | 215         | 10/01/2015 |
     | 2      | 125         | 10/01/2015 |
     | 1      | 225         | 10/04/2015 |
     | 2      | 150         | 10/04/2015 |
     | 1      | 250         | 10/08/2015 |


#### output


    | Object | Total  |   Date    |
    |-----------------------------|
    |  1     | 690    | 10/01/2015 |
    |  2     | 275    | 10/01/2015 |


# 40
### [Select rows with max value of a column](http://stackoverflow.com/questions/20888384/select-rows-with-max-value-of-a-column)
#### description


  I need to fetch the data ignoring the least alt_bill_id if there are two rows for same acct_id. In this case I need to ignore the row for acct_id 12345 and alt_bill_id 101. I need a result like the following:


#### input


    |  acct_id | Bill_Id |  Bill_dt   | alt_bill_id |
    |-----------------------------------------------|
    |  12345   |  123451 | 2014-01-02 |   101       |
    |  12345   |  123452 | 2014-01-02 |   102       |
    |  12346   |  123461 | 2014-01-02 |   103       |
    |  12347   |  123471 | 2014-01-02 |   104       |


#### output


    |  acct_id | Bill_Id |   Bill_dt  | alt_bill_id |
    |-----------------------------------------------|
    |  12345   |  123452 | 2014-01-02 |  102        |
    |  12346   |  123461 | 2014-01-02 |  103        |
    |  12347   |  123471 | 2014-01-02 |  104        |


# 41
### [GROUP BY with MAX(DATE)](http://stackoverflow.com/questions/3491329/group-by-with-maxdate)
#### description
  
    I'm trying to list the latest destination (MAX departure time) for each train in a table


#### input


    | Train | Dest | Time  |
    |----------------------|
    | 1     | HK   | 10:00 |
    | 1     | SH   | 12:00 |
    | 1     | SZ   | 14:00 |
    | 2     | HK   | 13:00 |
    | 2     | SH   | 09:00 |
    | 2     | SZ   | 07:00 |


#### output


    | Train | Dest | Time  |
    |----------------------|
    | 1     | SZ   | 14:00 |
    | 2     | HK   | 13:00 |


# 42
### [Finding all children for multiple parents in single SQL query](http://stackoverflow.com/questions/1563148/one-to-many-query-selecting-all-parents-and-single-top-child-for-each-parent)
#### description

    I want to select with single query every row from Parents table and for each one single row from Childs table with relation "parent"-"id" value and the greatest "feature" column value. In this example result should be.


#### input


    @Parents

    | id |   text  |
    |--------------|
    | 1  |  Blah   |
    | 2  |  Blah2  |
    | 3  |  Blah3  |

    @Childs

    | id | parent | feature |
    |-----------------------|
    | 1  |   1    |  123    |
    | 2  |   1    |   35    |
    | 3  |   2    |   15    |

#### output


    |p.id|p.text |c.id  |c.parent|c.feature|
    |--------------------------------------|
    |  1 | Blah  |  1   |    1   |    123  |
    |  2 | Blah2 |  3   |    2   |    15   |
    |  3 | Blah3 | null |   null |   null  |

      Where p = Parent table and c = Child table


# 43
### [SQL Select only rows with Minimum Value on a Column with Where Condition](http://stackoverflow.com/questions/29948321/sql-select-only-rows-with-minimum-value-on-a-column-with-where-condition)
#### description

    How can I select one row per productId with minimum orderIndex that not rejected?

#### input


    | id | productId | orderIndex | rejected |
    |----------------------------------------|
    | 1  |  1        |   0        |   1      |
    | 2  |  1        |   1        |   0      |
    | 3  |  1        |   2        |   0      |
    | 4  |  2        |   0        |   0      |
    | 5  |  2        |   1        |   1      |
    | 6  |  3        |   0        |   0      |


#### output


    | id | productId | orderIndex | rejected |
    |----------------------------------------|
    | 2  |  1        |   1        |   0      |
    | 4  |  2        |   0        |   0      |
    | 6  |  3        |   0        |   0      |


# 44
### [Selecting all corresponding fields using MAX and GROUP BY](http://stackoverflow.com/questions/1305056/selecting-all-corresponding-fields-using-max-and-group-by)
#### description

    And I would like to make a request that would return for each deal_id the row with the highest timestamp, and the corresponding status_id.


#### input


    | deal_id  | status_id|  timestamp           |
    |--------------------------------------------|
    | 1226     |    1     |  2009-08-17 09:29:00 |
    | 1226     |    2     |  2009-08-17 17:01:56 |
    | 1226     |    2     |  2009-08-18 12:10:07 |
    | 1226     |    3     |  2009-08-18 12:10:25 |
    | 1226     |    4     |  2009-08-17 15:52:19 |
    | 1227     |    1     |  2009-08-17 09:56:31 |
    | 1227     |    2     |  2009-08-17 14:31:25 |



#### output


    | c1    | c2 | c3                  |
    |----------------------------------|
    | 1226  | 3  | 2009-08-18 12:10:25 |
    | 1227  | 2  | 2009-08-17 14:31:25 |


# 45
### [In SQL, how to select the top 2 rows for each group](http://stackoverflow.com/questions/15969614/in-sql-how-to-select-the-top-2-rows-for-each-group)
#### description


    The aggregation function for group by only allow me to get the highest score for each name. I would like to make a query to get the highest 2 score for each name, how should I do?


#### input


    | NAME  | SCORE |
    |---------------|
    | willy |     1 |
    | willy |     2 |
    | willy |     3 |
    | zoe   |     4 |
    | zoe   |     5 |
    | zoe   |     6 |


#### output


    | NAME  | SCORE |
    |---------------|
    | willy |     2 |
    | willy |     3 |
    | zoe   |     5 |
    | zoe   |     6 |


# 46
### [Need to retrieve all records in table A and only single one in table B that is the last updated](http://stackoverflow.com/questions/29988267/need-to-retrieve-all-records-in-table-a-and-only-single-one-in-table-b-that-is-t)
#### description

    I have to retrieve certain records in TABLE_A - then need to display the last time the row was updated - which is in TABLE_B (however, there are many records that correlate in TABLE_B). TABLE_A's TABLE_A.PK is ID and links to TABLE_B through TABLE_B.LINK, where the schema would be:


#### input


    @table1

    | ID  | DESC         |
    |--------------------|
    | 100 | DESCRIPTION0 |
    | 101 | DESCRIPTION1 |

    @table2

    | ID | LINK | LAST_DATE  |
    |------------------------|
    | 1  | 100  | 12/12/2012 |
    | 2  | 100  | 12/13/2012 |
    | 3  | 100  | 12/14/2013 |
    | 4  | 101  | 12/12/2012 |
    | 5  | 101  | 12/13/2012 |
    | 6  | 101  | 12/14/2013 |


#### output


     @Result

    | c1  | c2           | c3         |
    |---------------------------------|
    | 100 | DESCRIPTION0 | 12/14/2013 |
    | 101 | DESCRIPTION1 | 12/14/2013 |


# 47
### [SQL get id of max bill per waiter](http://stackoverflow.com/questions/32930900/sql-get-id-of-max-bill-per-waiter/32931068#32931068)
#### description

    and I am looking for the id of the bill with the maximum amount per waiter.


#### input


    @bills table

    | id  | amount  | id_waiter |
    |---------------------------|
    | 1   | 20      | 1         |
    | 2   | 25      | 2         |
    | 3   | 50      | 2         |
    | 4   | 20      | 1         |
    | 5   | 60      | 1         |
    | 6   | 10      | 2         |


#### output


    | waiter | maxamount | bill |
    |---------------------------|
    | 1      | 60        | 5    |
    | 2      | 50        | 3    |


# 48
### [Select latest data per group from joined tables](http://stackoverflow.com/questions/29803154/select-latest-data-per-group-from-joined-tables)
#### description

    How can I get latest value, based on survey timestamp and grouped by store_code, product_code, and production_month?


#### input


    @table1

    | survey_id | store_code | timestamp  |
    |-------------------------------------|
    | 1         | store_1    | 2015-04-20 |
    | 2         | store_1    | 2015-04-22 |
    | 3         | store_2    | 2015-04-21 |
    | 4         | store_2    | 2015-04-22 |

    @table2

    | survey_id | product_code | production_month | value |
    |-----------------------------------------------------|
    | 1         | product_1    | 2                | 15    |
    | 2         | product_1    | 2                | 10    |
    | 1         | product_1    | 3                | 20    |
    | 1         | product_2    | 2                | 12    |
    | 3         | product_2    | 2                | 23    |
    | 4         | product_2    | 2                | 17    |


#### output


    | survey_id | store_code | time_stamp | product_code | production_month | value |
    |-------------------------------------------------------------------------------|
    | 2         | store_1    | 2015-04-22 | product_1    | 2                | 10    |
    | 1         | store_1    | 2015-04-20 | product_1    | 3                | 20    |
    | 1         | store_1    | 2015-04-20 | product_2    | 2                | 12    |
    | 4         | store_2    | 2015-04-22 | product_2    | 2                | 17    |


# 49
### [selecting max values grouped by two column](http://stackoverflow.com/questions/30361275/selecting-max-values-grouped-by-two-column)
#### description

    I want to select the 3 max payments grouped by the couple firstname, lastname

#### input


    | firstname | lastname | nb_payments |
    |------------------------------------|
    | a         | b        | 10          |
    | a         | b        | 20          |
    | b         | a        | 30          |
    | b         | a        | 40          |
    | b         | b        | 50          |

#### output


    | firstname | lastname | top3 |
    |-----------------------------|
    | b         | a        | 70   |
    | b         | b        | 50   |
    | a         | b        | 30   |


# 50
### [SQL Query and filtering data](http://stackoverflow.com/questions/31112142/sql-query-and-filtering-data/31112305#31112305)
#### description

    The validity of a time zone is depends on time_start field in the database. This is important to get the correct time_start.
    The example below shows how to query the time zone information using zone name America/Los_Angeles.

    SELECT * FROM timezone
    WHERE time_start < strftime('%s', 'now')
    AND zone_name='America/Los_Angeles'
    ORDER BY time_start DESC LIMIT 1;
    This query returns

    391|America/Los_Angeles|1425808800
    I'd like to to do the same thing but for all zone_id with one SQL Query.


#### input


    | zone_id | zone_name           | time_start |
    |--------------------------------------------|
    | 391     | America/Los_Angeles | 2147397247 |
    | 391     | America/Los_Angeles | 1425808800 |
    | 391     | America/Los_Angeles | 2140678800 |
    | 391     | America/Los_Angeles | 9972000    |
    | 392     | America/Metlakatla  | 2147397247 |
    | 392     | America/Metlakatla  | 436352400  |
    | 392     | America/Metlakatla  | 9972000    |
    | 393     | America/Anchorage   | 2147397247 |
    | 393     | America/Anchorage   | 2140682400 |
    | 393     | America/Anchorage   | 2120122800 |
    | 393     | America/Anchorage   | 1425812400 |
    | 393     | America/Anchorage   | 9979200    |


#### output

    | c1  | c2                  |   c3         |
    |------------------------------------------|
    | 391 | America/Los_Angeles | 1425808800   |
    | 392 | America/Metlakatla  | 436352400    |
    | 393 | America/Anchorage   | 1425812400   |


# 51
### [PostgreSQL - MAX value for every user](http://stackoverflow.com/questions/33063073/postgresql-max-value-for-every-user)

#### description

    I want to select MAX value for every user, than it also lower than a tresshold 8.

#### input

    | User  | Phone | Value |
    |-----------------------|
    | Peter | 0     | 1     |
    | Peter | 456   | 2     |
    | Peter | 456   | 3     |
    | Paul  | 456   | 7     |
    | Paul  | 789   | 10    |

#### output

    |  c1   | c2  | c3 |
    |------------------|
    | Peter | 456 | 3  |
    | Paul  | 456 | 7  |

# 52
### [SQL Greatest N Per Group with Extra Criteria](http://stackoverflow.com/questions/20282081/sql-greatest-n-per-group-with-extra-criteria)

#### description

    The current query would be:

    SELECT Person, SUM(Time) AS Duration 
    FROM Table
    GROUP BY Person

    What I need to add to this result set is the Uniq and value of the largest value per person

#### input

    | Uniq |  Value | Time | Person |
    |-------------------------------|
    |   1  |  6     | 180  | Bob    |
    |   2  |  8     | 170  | Bob    |
    |   3  |  4     | 45   | Claire |
    |   4  |  4     | 90   | Claire |

#### output

    | Person | Duration | Value | Uniq |
    |----------------------------------|
    | Bob    | 350      | 8     | 2    |
    | Claire | 135      | 4     | 3    |

# 53
### [greatest n per group needed in compound Mysql join sql](http://stackoverflow.com/questions/29080006/greatest-n-per-group-needed-in-compound-mysql-join-sql)

#### description
    
    What I need is the results to only contain the row with the greatest value in the quantity column and also the row with the greatest retail_price. So my result set I need would look like this

#### input

    | number|quantity|retail_price|
    |-----------------------------|
    | 1007  | 288    | 5.750      |
    | 1007  | 48     | 5.510      |
    | 1007  | 576    | 5.460      |
    | 1007  | 96     | 5.240      |
    | 1007  | 576    | 5.230      |
    | 1007  | 144    | 5.120      | 
    | 1006  | 200    | 5.760      |
    | 1006  | 100    | 5.550      |
    | 1006  | 200    | 5.040      |
    | 1006  | 500    | 5.010      |  

#### output

    | number|quantity|retail_price|
    |-----------------------------|
    | 1006  | 500    | 5.010      |
    | 1007  | 576    | 5.460      |   

# 54
### [Find duplicate in column tied to another column...](http://www.sqlteam.com/forums/topic.asp?TOPIC_ID=201063)


#### description
    
    For each unique PID, I need to find the PID's that have more than one GUID and then match that with their respective FirstName and LastName from the other table.

#### input

    @Table1

    | GUID  | PID  |
    |--------------| 
    | GUID1 | PID1 |
    | GUID1 | PID1 |
    | GUID1 | PID1 |
    | GUID2 | PID1 |
    | GUID3 | PID2 |
    | GUID3 | PID2 |
    | GUID3 | PID2 |


    @Table2

    | GUID  | LastName | FirstName |
    |------------------------------|
    | GUID1 | Mulder   |  Fox      |
    | GUID2 | Scully   |  Dana     |    
    | GUID3 | Skinner  |  Walter   |    

#### output
    
    | c1   |   c2   |     c3      |
    |-----------------------------|
    | PID1 | GUID 1 | Mulder Fox  |  
    | PID1 | GUID 2 | Scully Dana |

# 55
### [One to Many Query Issue](http://www.sqlteam.com/forums/topic.asp?TOPIC_ID=201008)

#### description

    I want to return all records for attribute A that do not contain a specific value (8) for attribute B. 

#### input

    | ID_NUM | ID_Status |
    |--------------------|
    | 123    | 5         |  
    | 123    | 6         |  
    | 123    | 6         |  
    | 123    | 10        |  

#### output

    | c   |
    |-----|
    | 123 |    

# 56
### [Max of Sum](http://www.sqlteam.com/forums/topic.asp?TOPIC_ID=200894)

#### description

    I need to write a query (Oracle) that will return the clientID and the creditCard type with the highest spending :

#### input

    | client | creditCard | amount |
    |------------------------------|
    | 1      |     A      | 10     |  
    | 1      |     A      | 20     |  
    | 1      |     B      | 40     |  
    | 1      |     C      | 5      |     
    | 1      |     C      | 10     |  
    | 1      |     C      | 20     |  
    | 2      |     A      | 40     |  
    | 2      |     D      | 60     |  

#### output

    | client | creditCard   |
    |-----------------------|
    |     1  | B            |
    |     2  | D            |


# 57
### [group by](http://www.sqlteam.com/forums/topic.asp?TOPIC_ID=200861)

#### input

    | chapterid | xmlfile |
    |---------------------|
    | 1234      | 123.xml |
    | 1234      | 123.xml |
    | 1234      | 123.xml |
    | 1234      | 123.xml |
    | 4567      | 123.xml |
    | 4567      | 123.xml |
    | 6789      | 145.xml |
    | 7890      | 234.xml |
    | 7890      | 234.xml |
    | 7890      | 234.xml |

#### description

    I would need an output that lists the distinct number of chapterids for each xmlfile

#### output

    | chapterid | xmlfile |
    |---------------------|
    | 1234      | 123.xml | 
    | 4567      | 123.xml |
    | 6789      | 145.xml |
    | 7890      | 234.xml |

# 58
### [group by]((http://www.sqlteam.com/forums/topic.asp?TOPIC_ID=200861))

#### input

    | chapterid | xmlfile |
    |---------------------|
    | 1234      | 123.xml |
    | 1234      | 123.xml |
    | 1234      | 123.xml |
    | 1234      | 123.xml |
    | 4567      | 123.xml |
    | 4567      | 123.xml |
    | 6789      | 145.xml |
    | 7890      | 234.xml |
    | 7890      | 234.xml |
    | 7890      | 234.xml |

#### description

    I would also need an output that lists the xmlfile with more than one chapterid

#### output

    | chapterid | xmlfile |
    |---------------------|
    | 1234      | 123.xml | 
    | 4567      | 123.xml |
    | 7890      | 234.xml |

# 59
### [Not Sure If this is a grouping question]( )

#### input

    | ID|  X | Y |
    |------------|
    | 1 | 25 | 24|
    | 2 | 24 | 25|
    | 3 | 75 | 1 |
    | 4 | 9  | 10|
    | 5 | 10 | 9 | 


#### output

    | ID | X | Y  |
    |-------------|
    | 2  |24 | 25 |
    | 3  |75 | 1  |
    | 5  |10 | 9  | 

#### description

    When values for X and Y exists, it will always show the higher ID value.

# 60
### [how to create distinct on 3 columns](http://www.tek-tips.com/viewthread.cfm?qid=1740423)

#### description
    Check if they are same in the subsequent rows having same ID, if Yes then the output should show it as one row only, if not display it as how it is.

#### input

    | ID | Name | Row | Exp |  time  | rowcount |
    |-------------------------------------------|
    | 1  |  A   |  50 |  0  |  17:00 |  1       |
    | 1  |  A   |  50 |  0  |  17:01 |  2       |
    | 2  |  B   |  10 |  2  |  17:02 |  1       |
    | 2  |  B   |  20 |  10 |  17:03 |  2       |
    | 2  |  B   |  20 |  10 |  17:04 |  3       |

#### output

    | ID | Name | Row | Exp | time  | rowcount |
    |------------------------------------------|
    | 1  |  A   |  50 |  0  | 17:00 |  1       | 
    | 2  |  B   |  10 |  2  | 17:02 |  1       |
    | 2  |  B   |  20 |  10 | 17:03 |  2       |


＃ 61
### [Compare avg of data in column of certain users against a another user](http://www.dbforums.com/showthread.php?1707382-Compare-avg-of-data-in-column-of-certain-users-against-a-another-user)

#### description

    I would like to find the average for each Month for User 1 and 2 and compare to User 3 for the same month so ideally I want 

#### input

    | trans | user | Month |
    |----------------------| 
    | 100   |  1   |    1  | 
    | 102   |  2   |    1  | 
    | 103   |  3   |    1  | 
    | 100   |  1   |    2  | 
    | 103   |  2   |    2  | 
    | 103   |  3   |    2  | 
    | 104   |  1   |    3  | 
    | 104   |  2   |    3  | 
    | 101   |  3   |    3  |

#### output

    | Avgtrans | user3Trans | Month | 
    | 101      |      103   |   1   |
    | 101.5    |      103   |   2   |
    | 104      |      101   |   3   |

# 62
### [SQL Server: How to Join to first row](http://stackoverflow.com/questions/2043259/sql-server-how-to-join-to-first-row)

#### description

    No description
    
#### input
    
    @Orders

    | LineItemGUID |  Order ID | Quantity | Description                 |  
    |-------------------------------------------------------------------|
    | {098FBE3...} |  1        | 7        | prefabulated amulite        |  
    | {1609B09...} |  2        | 32       | spurving bearing            |  
    | {A58A1...}   | 6,784,329 | 5        | pentametric fan             |
    | {0E9BC...}   | 6,784,329 | 5        | differential girdlespring   |

    @LineItems

    | OrderGUID | OrderNumber |
    |-------------------------|
    | {FFB2...} | STL-7442-1  |     
    | {3EC6...} | MPT-9931-8A |

#### output

    | OrderNumber | Quantity | Description               |  
    |----------------------------------------------------|  
    | STL-7442-1  | 7        | prefabulated amulite      |  
    | MPT-9931-8A | 32       | differential girdlespring | 
    | KSG-0619-81 | 5        | panametric fan            |  

# 63
### [SQL - find records from one table which don't exist in another](http://stackoverflow.com/questions/367863/sql-find-records-from-one-table-which-dont-exist-in-another)

#### description

    How do I find out which calls were made by people whose phone_number is not in the Phone_book? The desired output would be:

#### input

    @ Phone_book
    | id | name | phone_number |
    |--------------------------|
    | 1  | John | 111111111111 |
    | 2  | Jane | 222222222222 |

    @ Call
    | id | date | phone_number |
    |--------------------------|
    | 1  | 0945 | 111111111111 |
    | 2  | 0950 | 222222222222 |
    | 3  | 1045 | 333333333333 |

#### output

    @ Call
    | id | date | phone_number |
    |--------------------------|
    | 3  | 1045 | 333333333333 |

