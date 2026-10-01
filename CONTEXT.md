# Масса

A personal Android app for tracking body mass. It keeps a journal of measurements: one Weight per calendar date, editable, and enterable for past days.

## Language

**Record**:
A body-mass figure fixed against one calendar date. The date is the natural key: there is at most one Record per date.
_Avoid_: measurement, weigh-in, entry, log, reading

**Weight**:
The numeric body-mass value held by a Record, in kilograms to one decimal place.
_Avoid_: mass, figure, value, result

**Record Date**:
The calendar day a Record belongs to. The model holds no time of day: a Record describes a day, not a moment.
_Avoid_: timestamp, date taken, measurement date

**History**:
The complete list of Records in reverse chronological order — from the latest Record Date to the earliest.
_Avoid_: journal, log, list of weights

**Backdated Record**:
A Record created with a Record Date in the past rather than the current day.
_Avoid_: retroactive entry, past entry, historical entry
