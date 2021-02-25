package study.datajpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest{

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TeamRepository teamRepository;

    @PersistenceContext
    EntityManager em;

    @Test
    public void testMember() {
        Member member = new Member("MemberA");
        Member savedMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(savedMember.getId()).get();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    public void basicCRUD() {
        Member member1 = new Member("Member1");
        Member member2 = new Member("Member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();

        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        memberRepository.delete(memberRepository.getOne(member1.getId()));
        memberRepository.delete(memberRepository.getOne(member2.getId()));

        long deletedCount = memberRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    public void findByUserNameAndGreaterThan() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> result = memberRepository.findByUsernameAndAgeGreaterThan("AAA", 15);

        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void testQuery() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> result = memberRepository.findUser("AAA", 10);
        assertThat(result.get(0)).isEqualTo(m1);

    }

    @Test
    public void findUsernameList() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        Stream<String> usernameStream = Stream.of(m1, m2).map(Member::getUsername);

        List<String> usernameList = memberRepository.findUsernameList();

        assertThat(usernameList.stream().
                anyMatch(saved -> usernameStream.
                        anyMatch(created -> created.equals(saved))
        )).isTrue();

    }

    @Test
    public void findMemberDto() {
        Team team = new Team("teamA");
        teamRepository.save(team);

        Member m1 = new Member("AAA", 10);
        memberRepository.save(m1);
        m1.setTeam(team);

        List<MemberDto> memberDto = memberRepository.findMemberDto();

        assertThat(memberDto.stream().map(MemberDto::getUsername).
                anyMatch(saved -> saved.equals(m1.getUsername()))).isTrue();

    }

    @Test
    public void findByNames() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        Stream<Member> memberStream = Stream.of(m1, m2);

        List<Member> memberList = memberRepository.findByNames(List.of(m1.getUsername(), m2.getUsername()));

        assertThat(memberList.stream().
                anyMatch(saved -> memberStream.
                        anyMatch(created -> created.equals(saved))
                )).isTrue();
    }

    @Test
    public void returnType() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> foundMemberList = memberRepository.findListByUsername(m1.getUsername());
        Member foundMember = memberRepository.findMemberByUsername(m1.getUsername());
        Optional<Member> foundOptionalMember = memberRepository.findOptionalByUsername(m1.getUsername());

        assertThat(foundMemberList.stream().anyMatch(m -> m.equals(foundMember))).isTrue();
        assertThat(foundMemberList.stream().filter(m -> m.equals(foundMember)).count()).isEqualTo(1);
        assertThat(foundOptionalMember.get()).isEqualTo(foundMember);

        List<Member> nullResultList = memberRepository.findListByUsername("asdfdf");
        assertThat(nullResultList).isEmpty();

        Member nullResultMember = memberRepository.findMemberByUsername("awsdsdf");
        assertThat(nullResultMember).isNull();

        Optional<Member> optionalResultMember = memberRepository.findOptionalByUsername("asdfsdf");
        assertThat(optionalResultMember).isEqualTo(Optional.empty());


    }

    @Test
    public void paging() {
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        Page<Member> memberPage = memberRepository.findByAge(age, pageRequest);

        List<Member> content = memberPage.getContent();
        long totalElements = memberPage.getTotalElements();

        assertThat(content.size()).isEqualTo(3);
        assertThat(totalElements).isEqualTo(5);
        assertThat(memberPage.getNumber()).isEqualTo(0);
        assertThat(memberPage.getTotalPages()).isEqualTo(2);
        assertThat(memberPage.isFirst()).isTrue();
        assertThat(memberPage.hasNext()).isTrue();

        Slice<Member> memberSlice = memberRepository.findByUsernameLike("member", pageRequest);

    }

    @Test
    public void bulkUpdate() {
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 19));
        memberRepository.save(new Member("member3", 20));
        memberRepository.save(new Member("member4", 22));
        memberRepository.save(new Member("member5", 41));

        int resultCount = memberRepository.bulkAgePlus(20);

        assertThat(resultCount).isEqualTo(3);
        assertThat(memberRepository.findMemberByUsername("member5").getAge()).isEqualTo(42);
    }

    @Test
    public void findMemberLazy() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);
        Member m1 = new Member("member1", 10, teamA);
        Member m2 = new Member("member2", 10, teamB);
        memberRepository.save(m1);
        memberRepository.save(m2);

        em.flush();
        em.clear();

        List<Member> members = memberRepository.findAll();
        List<String> stringList = List.of(m1.getUsername(), m2.getUsername());
        assertThat(members.stream().map(Member::getUsername).anyMatch(m -> stringList.stream().anyMatch(e -> e.equals(m)))).isTrue();
    }

    @Test
    public void queryHint() {
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);
        em.flush();
        em.clear();

        Member findMember = memberRepository.findReadOnlyByUsername("member1");
        findMember.setUsername("member2");

        em.flush();
    }

    @Test
    public void lock() {
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);
        em.flush();
        em.clear();

        List<Member> memberList = memberRepository.findLockByUsername("member1");

    }

    @Test
    public void callCustom() {
        List<Member> result = memberRepository.findMemberCustom();
    }
}