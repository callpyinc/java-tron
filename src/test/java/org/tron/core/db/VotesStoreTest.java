package org.tron.core.db;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Vote;

@Slf4j
public class VotesStoreTest {

  private static final String dbPath = "output-votesStore-test";
  private static AnnotationConfigApplicationContext context;
  VotesStore votesStore;
  WitnessStore witnessStore;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  @Before
  public void initDb() {
    this.votesStore = context.getBean(VotesStore.class);
    this.witnessStore = context.getBean(WitnessStore.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void putAndGetVotes() {
    List<Vote> oldVotes = new ArrayList<Vote>();

    VotesCapsule votesCapsule = new VotesCapsule(ByteString.copyFromUtf8("100000000x"), oldVotes);
    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);

    Assert.assertEquals(votesStore.getAllVotes().size(), 1);
    Assert.assertTrue(votesStore.has(votesCapsule.createDbKey()));
    VotesCapsule votesSource = this.votesStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), votesSource.getAddress());

//    votesCapsule = new VotesCapsule(ByteString.copyFromUtf8(""), oldVotes);
//    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);
//    votesSource = this.votesStore.get(ByteString.copyFromUtf8("").toByteArray());
//    Assert.assertEquals(votesStore.getAllVotes().size(), 2);
//    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
//    Assert.assertEquals(null, votesSource.getAddress());
  }


  private String createAddress(Integer i) {
    // 1 ~ 999999
    String result = "aaaaaa";
    String is = i.toString();
    while (i / 10 != 0) {
      result = result.substring(0, result.length() - 1);
      i = i / 10;
    }
    return result + is;
  }

  private String createWitnessAddress(Integer i) {
    String OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1" + createAddress(i);
    return OWNER_ADDRESS_FIRST;
  }

  private WitnessCapsule createWitness(Integer i) {
    String OWNER_ADDRESS_FIRST = createWitnessAddress(i);

    WitnessCapsule witnessCapsule = new WitnessCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
        1_000_000_000L,//10亿
        "");
    return witnessCapsule;
  }


  //30 oldVote,30 newVote
  private VotesCapsule createVote(Integer in) {

    List<Vote> oldVotes = new ArrayList<Vote>();
    List<Vote> newVotes = new ArrayList<Vote>();

    for (int i = 1; i < 30; i++) {
      int x = (int) (Math.random() * 200);
      String OWNER_ADDRESS_FIRST = createWitnessAddress(x);
      Vote vote = Vote.newBuilder()
          .setVoteAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)))
          .setVoteCount(100L).build();
      oldVotes.add(vote);
    }

    for (int i = 1; i < 30; i++) {
      int x = (int) (Math.random() * 200);
      String OWNER_ADDRESS_FIRST = createWitnessAddress(x);
      Vote vote = Vote.newBuilder()
          .setVoteAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)))
          .setVoteCount(100L).build();
      newVotes.add(vote);
    }

    VotesCapsule votesCapsule = new VotesCapsule(ByteString.copyFromUtf8(in.toString() + "x"),
        oldVotes, newVotes);

    return votesCapsule;
  }


  private Map<ByteString, Long> countVote(VotesStore votesStore) {
    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    final List<VotesCapsule> votesList = votesStore.getAllVotes();
    final List<Integer> list = new ArrayList();
    list.add(0);

    votesList.forEach(votes -> {
      votes.getOldVotes().forEach(vote -> {
        //TODO validate witness //active_witness
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        if (countWitness.containsKey(voteAddress)) {
          countWitness.put(voteAddress, countWitness.get(voteAddress) - voteCount);
        } else {
          countWitness.put(voteAddress, -voteCount);
        }
      });
      votes.getNewVotes().forEach(vote -> {
        //TODO validate witness //active_witness
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        if (countWitness.containsKey(voteAddress)) {
          countWitness.put(voteAddress, countWitness.get(voteAddress) + voteCount);
        } else {
          countWitness.put(voteAddress, voteCount);
        }
      });

      list.set(0, list.get(0) + 1);
      if (list.get(0) % 1000 == 0) {
        System.out.println("countVote i : " + list.get(0));
      }

    });
    return countWitness;
  }

  @Test
  public void calculateVoteTest() {

    //200个witness，100W个账户投票，

    //1、创建200个witness
    //2、创建100W个账户投票
    //start
    //3、统计
    //end

    votesStore.reset();

    for (int i = 1; i < 200; i++) {
      WitnessCapsule witnessCapsule = createWitness(i);
      witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    }

    for (int i = 1; i < 1000000; i++) {
      VotesCapsule votesCapsule = createVote(i);
      votesStore.put(votesCapsule.createDbKey(), votesCapsule);
      if (i % 1000 == 0) {
        System.out.println("VotesCapsule i : " + i);
      }
    }

    long time = 0;
    for (int i = 0; i < 3; i++) {
      long startTime1 = System.currentTimeMillis();
      votesStore.getAllVotes();
      time += (System.currentTimeMillis() - startTime1);
    }
    System.out.println("getAllVotes time: " + time / 3);

    time = 0;
    for (int i = 0; i < 3; i++) {
      long startTime1 = System.currentTimeMillis();
      countVote(votesStore);
      time += (System.currentTimeMillis() - startTime1);
    }
    System.out.println("countVote time: " + time / 3);


  }


}