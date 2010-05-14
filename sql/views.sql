drop view if exists CONTRIBUTION_VIEW;
create view CONTRIBUTION_VIEW as
select 
	ctb_year, ctb_month, ctb_day, 
	clb_login, clb_first_name, clb_last_name, 
	tsk_path, tsk_code, tsk_name,
	ctb_duration
from collaborator, contribution, task
where 
	ctb_contributor=clb_id
	and ctb_task=tsk_id
order by
	ctb_year, ctb_month, ctb_day, clb_id, tsk_path;