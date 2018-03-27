package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.Player.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.security.auth.callback.ConfirmationCallback;

import org.omg.IOP.TAG_RMI_CUSTOM_MAX_STREAM_FORMAT;

import java.util.*; // TODO: figure out what we actually need to import here (to solve errors for ImmutableGraph etc)
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.UndirectedGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {
	private List<Boolean> mRounds;
	private Graph<Integer, Transport> mGraph;
	private ArrayList<ScotlandYardPlayer> mPlayers = new ArrayList<ScotlandYardPlayer>();
	private int mCurrentRound = NOT_STARTED;
	private int mMrXLastLocation = 0;

	//Constructor
	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph, PlayerConfiguration mrX,
			PlayerConfiguration firstDetective, PlayerConfiguration... restOfTheDetectives) {
		this.mRounds = requireNonNull(rounds);
		this.mGraph = requireNonNull(graph);

		if (mRounds.isEmpty()) {
			throw new IllegalArgumentException("Empty mRounds");
		}

		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}

		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}

		// Loop over all detectives in temporary list to validate
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>(); // tempory list for validation

		configurations.add(mrX);
		configurations.add(firstDetective);
		// Create List of ScotlandYardPlayers (mutable)

		// add configurations to temporary list
		for (PlayerConfiguration configuration : restOfTheDetectives) {
			configurations.add(requireNonNull(configuration));
		}

		// start processing all configurations
		// data stores for processed data
		Set<Integer> locations = new HashSet<>();
		this.mPlayers = new ArrayList<>();
		Set<Colour> colours = new HashSet<>();

		for (PlayerConfiguration configuration : configurations) {
			// Check if players have duplicated locations
			if (locations.contains(configuration.location)) {
				throw new IllegalArgumentException("Duplicate location");
			}
			locations.add(configuration.location);
			if (colours.contains(configuration.colour)) {
				throw new IllegalArgumentException("Duplicate colour");
			}
			colours.add(configuration.colour);

			// ticket check
			if (configuration.tickets.get(BUS) == null) {
				throw new IllegalArgumentException("Detective is missing BUS tickets");
			}

			if (configuration.tickets.get(TAXI) == null) {
				throw new IllegalArgumentException("Detective is missing TAXI tickets");
			}

			if (configuration.tickets.get(UNDERGROUND) == null) {
				throw new IllegalArgumentException("Detective is missing UNDERGROUND tickets");
			}

			if (configuration.colour.isDetective()) {
				if (requireNonNull(configuration.tickets.get(SECRET)) != 0) {
					throw new IllegalArgumentException("Detective should not have secret tickets");
				}
				if (requireNonNull(configuration.tickets.get(DOUBLE)) != 0) {
					throw new IllegalArgumentException("Detective should not have secret tickets");
				}
			}

			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour,
					configuration.location, configuration.tickets);
			this.mPlayers.add(player);
		}

	}

	//End of Constructor
	public Integer getMrXLocation() {
		return this.mMrXLastLocation;
	}

	/**
	 * Returns the colour of the next player to play this round
	 */
	private Colour nextPlayer(Colour current) {
		List<Colour> players = getPlayers();
		Colour result = BLACK; // initialise as black just incase
		int currentIndex = -1;

		for (Colour player : players) {
			if (player == current) {
				currentIndex = players.indexOf(player);
				if (currentIndex < players.size() - 1) {
					result = players.get(currentIndex + 1);
				}
			}
		}

		if (currentIndex < 0) {
			throw new RuntimeException("nextPlayer unable to generate index for Colour " + current);
		}

		return result;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		if (this.isGameOver()) {
			throw new IllegalStateException("startRotate called but the game is already over!");
		}

		// obtain current player
		// notify player to move via Player.makeMove
		// TODO: use empty list OR a list containing a single PassMove
		// TODO: replace fake list with generated valid mves

	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> result = new ArrayList<>();
		for (ScotlandYardPlayer player : this.mPlayers) {
			result.add(player.colour());
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		HashSet<Colour> winners = new HashSet<Colour>();
		Integer mrXlocation = 0;

		for (ScotlandYardPlayer player : this.mPlayers) {
			if (player.colour() == BLACK) {
				mrXlocation = player.location();
			} else {
				if (player.location() == mrXlocation) {
					winners.add(player.colour());
				}
			}
		}

		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> requestedLocation = Optional.empty();
		boolean playerFound = false;

		// look for the player
		for (ScotlandYardPlayer player : this.mPlayers) {
			if (player.colour() == colour) {
				playerFound = true;
				if (colour != BLACK) {
					requestedLocation = Optional.of(player.location());
				} else {
					requestedLocation = Optional.of(this.getMrXLocation());
				}
			}
		}

		return requestedLocation;
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		Optional<Integer> result = Optional.empty();
		for (ScotlandYardPlayer player : this.mPlayers) {
			if (player.colour() == colour) {
				result = Optional.of(player.tickets().get(ticket));
			}
		}

		return result;
	}

	@Override
	public boolean isGameOver() {
		// TODO
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return (BLACK);
	}

	@Override
	public int getCurrentRound() {
		return mCurrentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(mRounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<Integer, Transport>(mGraph);
	}

}
