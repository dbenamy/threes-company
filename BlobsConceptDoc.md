# Blobs Concept Doc #

## Intro ##
Blobs is a turn based strategy game played on a 2D board. 2 players start at opposite ends of the board, each armed with: 2 push blobs; 2 pull blobs; one explosion blob; one death ray blob; and 10 action points. Each turn, a player gets 2 additional action points up to a maximum of 10, which they can spend to move blobs or use their abilities.  The amount of points used per action depends on the action's strength with points accruing after each turn. Players try to kill each others' blobs using a variety of tactics discussed in further detail below. A player wins when they have destroyed all enemy blobs.

## The Board ##
The board is a square with each side being 25 times the diameter of a starting blob. It does not have any grid visible to the players. The resolution of the board will be fine grained so that it will seem to players that they can move within a full 360 degree range of motion.  Blobs whose center crosses any edge of the board fall to their doom. Players can change the board by creating holes into which enemy blobs may be pushed or pulled.  The board can have features on it as well, such as anchor points (discussed below).

## Turns ##
A turn consists of a set up phase where the player decides how they will spend their AP, and an execution phase where the moves are carried out and the physics simulated. Conceptually, the second phase is just a bunch math which determines the new games state. In the game though, we will show the execution and simulation in realtime and it will be interesting to watch.

There will be an option to have time limits on turns to force the game to move quickly.

## Movement and Actions ##
Each turn, a player gets 2 action points (which they can accrue up to a maximum of 10) which they can spend to:
  * move or turn blobs,
  * use blob abilities,
  * split blobs, or
  * start or stop a blob's growth.
AP costs are descibed below. A player's turn is finished when the player has spent all their AP on blob actions. They can also end it earlier to carry the remaining AP to their next turn.

When two blobs (from different teams) touch, the larger destroys the smaller. If two blobs from the same team hit, they bounce off of each other.

During simulation, blobs move with speed inversely proportional to their size.

When blob abilities are activated, they stay active for one round (until it's that players turn again).

## Unit Growth ##
Blobs have a size. The starting units will be size 1. Every blob grows slightly each turn until they reach a maximum size of 3. When blobs split, each child blob is half the size of the parent blob. Blobs must be at least size 1 to split.

## Unit Types ##
There are a few kinds of blobs:
  * Pusher blobs
  * Puller blobs
  * Death ray blobs
  * Exploding blobs

**Force Blobs**

When pusher or puller blobs activate their abilities/forces, there is a force exerted in all directions between it and other blobs. For example, if a pusher blob is next to another blob and activates its force, both blobs will be pushed apart from each other.
If a blob is on an anchor point, it can't be moved by a blob force, but can still affect other blobs. Blob forces play a central role to the game strategy and can be used to sling blobs across the board, suck in weak enemy blobs, push and pull blobs off board edges (being careful not the sacrifice their own blobs), keep blobs out of an area, push blobs into the line of fire of death rays, etc.

**Death Rays**

When a death ray blob is activated, it emits death rays in 4 directions- forwards, backwards, left, and right where forwards is the direction it last moved in or was turned to. Any blob on either team that is touched by a death ray is killed. Activating the death ray will have a high AP cost (9?).

**Exploding**

An exploding blob destroys itself, creating a hole in the board the size of the blob. Any other blob that falls into the hole dies. If a blob who is at least as big as the hole moves onto it, the blob fills in the hole with itself, disappearing in the process. After that, any blob can once again move over that area.

## Unit Balance ##
  * The cost to move a blob (in AP) is proportional to its size. Favors small blobs.
  * The killing power of a blob when it touches another is its size. Favors big blobs.
  * The force a force blob exters is inversly proportional to its size (and the distance between the blobs being moved). Favors small blobs.
  * The amount a blob moves when subjected to a force is inversly proportional to its size. Favors big blobs.
  * The range of a death ray blob's rays is inversly proportional to its size. Favors small blobs.

## Strategies ##
Want to use big blobs for greater killing power vs want to use many blobs for abilities.
...

## One Minute of Gameplay ##

  * Players each start with 2 push blobs, 2 pull blobs, one explosion and one death ray blob; we'll call our players Blue and Red.
  * Turns move fairly quickly. Blue starts by setting his death ray and a pusher blob in motion toward Red's pieces.
  * Red reacts by moving forward his exploding blob and both puller blobs.
  * Blue decides to focus on a death ray strategy, splitting his death ray in half for increased numbers and for the longer range.
  * Red thinks about going for a explode-and-pull strategy; he splits his exploding blob to have a spare, and advances his pullers behind one exploding blob.
  * Blue sees what's happening and, anticipating being sucked into a hole next turn, activates a pusher to repel the incoming threat.  He also activates his death ray on one of Red's pullers, effectively cutting their sucking power in half.
  * Red, now rethinking his strategy, stops his exploding and puller blobs and splits the remaining puller.  With some Activity Points still remaining, and knowing that Blue is mostly powerless after using the death ray, Red decides to save the points for a stronger, coordinated strike next turn.
  * Blue knows he's left the door open for an attack.  He uses the few Activity Points accrued since last turn to disburse his units out of any formation, anticipating a death ray coming on Red's next turn.
  * Red, finding an opportunity for maximum havok, unleashes a guerrilla attack-and-retreat with his death ray...

## Platform ##
Blobs will be a PC game that runs on at least Windows and Linux. If we decide to write it in Java, it should be playable on any J2SE platform.

## Points of Differentiation ##
We don't know of any game like this. It's different than traditional strategy games like Chess and Checkers in that it involves physics simulations so you can't really plan a few moves ahead with precision and it will depend more on intuition and experience.

## Inspirational Sources ##
Elements of Blobs are inspired by a number of games. The turn based strategy play is inspired by Chess, the use of "physical" bodies interacting is from Pool, and the other interesting abilities and visual style is from Worms.

## Target Audience ##
Everyone... 8 to 80 years old.

## Competitive Products ##
Worms.

## Visual Style ##
Blobs is about thinking with abstract shapes and forces. There's nothing about it that needs to look realistic. It will have a simple and cartoony look and feel. Although the game is turn-based, we'd like to give a sense of motion and the forces involved, which we'll reflect with passive arrows and simple animated blobs. Simplicity is important, since ever-multiplying pieces will otherwise complicate the field of view.