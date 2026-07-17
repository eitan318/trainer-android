please create a plan for a simple android training app.
all with simple and efficient UI. code is minimal, simple and maintainable.
The app should support the following features:

# workout structure

global app settings for defaults: 
- rest time between sections
- rest time between exercizes,
- weight
- sets
- reps
- whether it is reps or duration
- duration
- set rest time
- volume-increment-precent
those are configureable

the default hirarchy:
- default vals
- workout override of defaults
- section override of workout defaults
- exercize override of workout defaults

* workout
    props: name, rest time between sections  
    funcs:  adding exiting archived exercize to workout, removing exercize from workout
    internal: in the normal view of the workout, show color indicator if the volume(reps-or-duration*sets*weight) 
        increments in [volume-increment-precent], or slower, or very slower, since the last volume reset of the exercize (a btn). 
        this only shows when viewing a workout... not on worout flow.

* workout sections
    props name, rest time between exercizes, optional unique rest time after
    funcs: adding a workout section, deleting a workout section,
    internal: current execution id (how many times was executed)

* exercize
    props: waight(default), [duration(default) / reps(default)], sets(default), set rest time(default), optional unique rest time after
    funcs: adding new exercize to archive, removing an exercize from archive, moving exercize between workout sections,


# exporting and importing:
    all workouts 
    all archived exercizes.

# starting a workout flow:
(full screen mode) - big fonts according to importance

on the top the set number like 1/3
show exercize name, reps, weight etc... ability to toggle read exercize description if there is.
    exercize:
        and button for starting rest.
    rest: 
        there is a big rest timer with the next exercize info.
        set number goes up. 
        if this was the last set, move to next exercize (displaied on rest too) 
when finished it shows workout finished.

What technology to use (I am developing on linux with nvim)?


