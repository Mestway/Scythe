http://forums.tutorialized.com/sql-basics-113/need-help-in-wiring-a-query-380750.html

I have a table by name provider and it has columns loginid,provider and city.
I want to write a query to get the loginid for which the provide count is more than 5.													
Thanks in advance for your help.													
										

=====
Expected SQL

select loginid
from provider
group by loginid
having count(*) > 5


============										
										

Input table	provider			Output table			query:						
loginid	provider	city		loginid									
1	2	NY		1									
1	3	LA											
1	4	SEA											
1	5												
1	9	SEA											
1	4	LA											
2	2	SEA											
2	3	BOS											
3	1	AT											
													
													
													
		select loginid from provider group by loginid having count(*) > 5											
