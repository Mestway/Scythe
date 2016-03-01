http://forums.tutorialized.com/sql-basics-113/finding-list-of-duplicate-records-with-more-than-two-duplicates-379845.html

//
The expected SQL:

select upedonid, count(upedonid)
from pedon
where upedonid != 'null'
group by upedonid
having count(upedonid) > 1;
//
