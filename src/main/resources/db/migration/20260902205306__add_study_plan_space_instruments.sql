create table study_plan_space_instruments (
  study_plan_space_instrument_id uuid primary key,
  institution_id uuid not null,
  study_plan_space_id uuid not null,
  instrument_id uuid not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint fk_study_plan_space_instruments_institution
    foreign key (institution_id) references institutions(institution_id),
  constraint fk_study_plan_space_instruments_space
    foreign key (study_plan_space_id) references study_plan_spaces(study_plan_space_id),
  constraint fk_study_plan_space_instruments_instrument
    foreign key (instrument_id) references instruments(instrument_id),
  constraint study_plan_space_instruments_institution_id_id_unique
    unique (institution_id, study_plan_space_instrument_id),
  constraint study_plan_space_instruments_space_instrument_unique
    unique (study_plan_space_id, instrument_id)
);

create index idx_study_plan_space_instruments_institution_id
  on study_plan_space_instruments (institution_id);

create index idx_study_plan_space_instruments_study_plan_space_id
  on study_plan_space_instruments (study_plan_space_id);

create index idx_study_plan_space_instruments_instrument_id
  on study_plan_space_instruments (instrument_id);
