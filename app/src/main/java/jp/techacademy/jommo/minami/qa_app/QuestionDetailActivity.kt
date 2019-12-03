package jp.techacademy.jommo.minami.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef:DatabaseReference
    private lateinit var mDatabaseReference: DatabaseReference

    private var mIsFavorite = false



    override fun onResume() {
        super.onResume()

        val user = FirebaseAuth.getInstance().currentUser

        if(user==null){
            favoButton.visibility = View.INVISIBLE

        }else{
            mFavoriteRef = mDatabaseReference.child(Favorite).child(user!!.uid)
            mFavoriteRef.addChildEventListener(fEventListener)
            favoButton.visibility = View.VISIBLE
            mIsFavorite = false
        }
    }

    private val fEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            val data = dataSnapshot.value as Map<*, *>?

            val favoriteUid = dataSnapshot.key ?: ""

            if(data != null) {

                for ((key, value) in data) {
                      if(mQuestion.questionUid==favoriteUid){
                        mIsFavorite = true
                    }
                }
            }

            if(mIsFavorite==false){
                favoButton.text = "お気に入りに追加"
            }else {
                favoButton.text = "お気に入りを解除"
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) { }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) { }

        override fun onCancelled(databaseError: DatabaseError) { }
    }


    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) { }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) { }

        override fun onCancelled(databaseError: DatabaseError) { }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title


        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        mDatabaseReference = FirebaseDatabase.getInstance().reference

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }


        favoButton.setOnClickListener {

            val user = FirebaseAuth.getInstance().currentUser
            val favoriteRef = mDatabaseReference.child(Favorite).child(user!!.uid).child(mQuestion.questionUid)

            val data = HashMap<String,String>()

            data["genre"] = mQuestion.genre.toString()

            if(mIsFavorite==true){
                favoriteRef.removeValue()
                mIsFavorite=false
                favoButton.text = "お気に入りに追加"
            }else{
                favoriteRef.setValue(data)
                mIsFavorite=true
                favoButton.text = "お気に入りを解除"
            }
        }


        mAnswerRef = mDatabaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

    }

}