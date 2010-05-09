-- Recherche des paires jour-collaborateur pour lesquelles
-- la somme des contributions est <> 1 jour
select ctb_year, ctb_month, ctb_day, clb_login, sum(ctb_duration) as ctb_sum
from contribution, task, collaborator
where tsk_id=ctb_task and ctb_contributor=clb_id
group by clb_id, ctb_year, ctb_month, ctb_day
having ctb_sum<>100 and ctb_sum<>0;

-- Cumul des jours par mois et par collaborateur
select ctb_year, ctb_month, clb_login, sum(ctb_duration)/100
from contribution, task, collaborator
where tsk_id=ctb_task and ctb_contributor=clb_id
group by clb_id, ctb_year, ctb_month, clb_login
order by ctb_year, ctb_month, clb_login

-- Cumul des consommations par tache
select tsk_path, tsk_code, sum(ctb_duration)/100
from contribution, task
where tsk_id=ctb_task 
group by tsk_id
order by tsk_path, tsk_code

-- Cumul des imputations sur le projet par collaborateur
-- et par mois
select ctb_year, ctb_month, clb_login, sum(ctb_duration)/100
from contribution, task, collaborator
where
  tsk_id=ctb_task and ctb_contributor=clb_id
  and left(tsk_path, 2)='01'
group by clb_id, ctb_year, ctb_month
order by ctb_year, ctb_month, clb_login

-- Cumul des imputations sur le projet
-- durant une période donnée
select sum(ctb_duration)/100
from contribution, task
where
  tsk_id=ctb_task
  and left(tsk_path, 2)='01'
  and ctb_year*10000+(ctb_month*100+ctb_day) 
  	between 20050501 and 20050617

-- Calcul des cumuls pour toutes les taches contenant
-- d'autres taches
select t1.tsk_name, sum(ctb_duration)/100
from contribution, task as t1, task as t2
where
  ctb_task=t2.tsk_id
  and t2.tsk_path=concat(t1.tsk_path, right(concat('0', hex(t1.tsk_number)), 2))
  and left(t2.tsk_path, 2)='01'
  and ctb_year*10000+(ctb_month*100+ctb_day) between 20050501 and 20050617
group by t1.tsk_id

-- Cumul par tache et par semaine
select 
	tsk_path, tsk_code, tsk_name,
	ctb_year, weekofyear(str_to_date(concat(ctb_year, right(concat('0', ctb_month), 2), right(concat('0', ctb_day), 2)), '%Y%m%d')) as weekofyear,
	sum(ctb_duration)/100
from contribution, task
where tsk_id=ctb_task
group by ctb_year, weekofyear, tsk_id
order by ctb_year, weekofyear, tsk_code