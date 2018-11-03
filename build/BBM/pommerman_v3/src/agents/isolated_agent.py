from .base_agent import MyBaseAgent
from pommerman import constants
import random
import numpy as np


verbose = False


class IsolatedAgent(MyBaseAgent):

    """
    This agent should be used when the agent is isolated from other agents via walls.
    This agent does not consider other agents.
    Just focus on breaking woods and collecting items.
    """
    
    def act(self, obs, action_space, info):

        #
        # Definitions
        #

        self._search_range = 10

        board = obs['board']
        my_position = obs["position"]  # tuple([x,y]): my position
        my_ammo = obs['ammo']  # int: the number of bombs I have
        my_blast_strength = obs['blast_strength']

        if verbose:
            print("My position", my_position, end="\t")


        # List of the set of survivable time-positions at each time
        # and preceding positions
        survivable, prev, succ, _ \
            = self._search_time_expanded_network(info["list_boards_no_move"],
                                                 my_position)

        # where to place bombs to break wood
        bomb_target, n_breakable \
            = self._get_bomb_target(info["list_boards_no_move"][-1],
                                    my_position,
                                    my_blast_strength,
                                    constants.Item.Wood)

        # Items that can be reached in a survivable manner
        reachable_items, _, next_to_items \
            = self._find_reachable_items(info["list_boards_no_move"],
                                         my_position,
                                         survivable,
                                         bomb_target)

        # Survivable actions
        is_survivable, survivable_with_bomb \
            = self._get_survivable_actions(survivable,
                                           obs,
                                           info["curr_bombs"],
                                           info["curr_flames"])

        survivable_actions = [a for a in is_survivable if is_survivable[a]]

        if verbose:
            print("survivable actions are", survivable_actions)

        #
        # Choose an action
        #

        if len(survivable_actions) == 0:

            # This should not happen
            return None

        elif len(survivable_actions) == 1:

            # move to the position if it is the only survivable position
            action = survivable_actions[0]
            print("The only survivable action", action)
            return action.value

        #
        # Place a bomb
        #

        consider_bomb = True
        if survivable_with_bomb is None:
            consider_bomb = False
        elif not bomb_target[my_position]:
            consider_bomb = False
        elif any([len(s) <= 0 for s in survivable_with_bomb]):
            # if not survivable all the time after bomb, do not bomb
            consider_bomb = False
        elif self._might_break_powerup(info["list_boards_no_move"][-1],
                                       my_position,
                                       my_blast_strength,
                                       info["might_powerup"]):
            # if might break an item, do not bomb
            consider_bomb = False

        if consider_bomb:
            # place bomb if I am at a bomb target
            print("Bomb at a bomb target", constants.Action.Bomb)
            return constants.Action.Bomb.value

        # Move towards good items
        # TODO : kick may be a good item only if I cannot kick yet
        # TODO : might want to destroy
        good_items = [constants.Item.ExtraBomb, constants.Item.IncrRange, constants.Item.Kick]

        # positions with good items
        good_time_positions = set()
        for item in good_items:
            good_time_positions = good_time_positions.union(reachable_items[item])
        if len(good_time_positions) > 0:
            action = self._find_distance_minimizer(my_position,
                                                   good_time_positions,
                                                   prev,
                                                   is_survivable)
            if action is not None:
                print("Moving toward good item", action)
                return action.value

        #
        # Move towards where to bomb
        #
        
        good_time_positions = reachable_items["target"]

        # If I have no bomb, I do not want to wait at the target that will be covered by flames
        # before I can place a bomb
        if my_ammo == 0:
            first_blast_time = constants.DEFAULT_BOMB_LIFE
            for t, x, y in reachable_items[constants.Item.Bomb]:
                life = obs["bomb_life"][(x,y)]
                if life < first_blast_time:
                    first_blast_time = life

            _good_time_positions = list()
            for t, x, y in good_time_positions:
                if any([t > first_blast_time,
                        info["list_boards_no_move"][int(first_blast_time)][(x, y)] != constants.Item.Flames.value]):
                    _good_time_positions.append((t, x, y))
            if _good_time_positions:
                good_time_positions = _good_time_positions
                
        action = self._find_distance_minimizer(my_position,
                                               good_time_positions,
                                               prev,
                                               is_survivable)
        if action is not None:
            print("Moving toward where to bomb", action)
            return action.value

        #
        # TODO : move toward might powerups
        #
        
        #
        # Move towards a fog where we have not seen longest
        #

        best_time_position = None
        oldest = 0
        for t, x, y in next_to_items[constants.Item.Fog]:
            neighbors = [(x+dx, y+dy) for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]]
            age = max([info["since_last_seen"][position] for position in neighbors if self._on_board(position)])
            if age > oldest:
                oldest = age
                best_time_position = (t, x, y)

        if best_time_position is not None:
            action = self._find_distance_minimizer(my_position,
                                                   [best_time_position],
                                                   prev,
                                                   is_survivable)
            if action is not None:
                print("Moving toward oldest fog", action)
                return action.value
        
        #
        # Random action
        #

        if constants.Action.Bomb in survivable_actions:
            survivable_actions.remove(constants.Action.Bomb)
        
        action = random.choice(survivable_actions)
        print("Random action", action, survivable_actions)
        return action.value
        
    
    
