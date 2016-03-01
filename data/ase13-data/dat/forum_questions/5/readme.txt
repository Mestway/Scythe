

http://forums.tutorialized.com/sql-basics-113/query-help-select-join-order-by-count-193192.html

//==============

expected SQL:

select entry.entryid, count(*)
from entry, vote
where entry.entryid = vote.entryid
group by entry.entryid

//==============



Entry: ID, Title, Post		
Vote: ID, EntryID, Result		

I want to be able to query the vote table for each entry and
see how many vote's there are, and then sort the entry's by
how many vote's each table had. I have messed around with joins,
etc. and cannot seem to figure it out. Any suggestions?
