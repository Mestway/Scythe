http://forums.tutorialized.com/sql-basics-113/sql-query-379547.html

///

Expected SQL:

select project_code, sum(invoice_amount)
from invoices
group by project_code

///


lets say,I have a table which holds invoices.
Fields are Project Code,invoice no,invoice amount.
I want to find total invoice amount for each project. 
any suggestions how? 

