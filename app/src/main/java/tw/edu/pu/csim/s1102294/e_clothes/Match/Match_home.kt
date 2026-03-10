package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Community.Liked_Post
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class Match_home : AppCompatActivity() {

    // MatchAdapter to bind data to RecyclerView
    class MatchAdapter(private var matchList: List<Match>) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.match_item, parent, false)
            return MatchViewHolder(view)
        }

        override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
            val match = matchList[position]
            holder.bind(match)
        }

        override fun getItemCount(): Int = matchList.size

        fun updateMatches(newMatches: List<Match>) {
            matchList = newMatches
            notifyDataSetChanged()
        }

        inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val matchNameTextView: TextView = itemView.findViewById(R.id.match_name)
            private val weatherCategoryTextView: TextView =
                itemView.findViewById(R.id.weather_category)
            private val hatImageView: ImageView = itemView.findViewById(R.id.hat_image)
            private val clothesImageView: ImageView = itemView.findViewById(R.id.clothes_image)
            private val pantsImageView: ImageView = itemView.findViewById(R.id.pants_image)
            private val shoesImageView: ImageView = itemView.findViewById(R.id.shoes_image)

            fun bind(match: Match) {
                // Log to ensure the correct match data is being bound
                Log.d("MatchViewHolder", "Binding match: $match")

                matchNameTextView.text = match.搭配名稱
                weatherCategoryTextView.text = match.天氣種類

                // Load images using Glide or other image loading methods
                Glide.with(itemView.context).load(match.帽子圖片網址).into(hatImageView)
                Glide.with(itemView.context).load(match.上衣圖片網址).into(clothesImageView)
                Glide.with(itemView.context).load(match.褲子圖片網址).into(pantsImageView)
                Glide.with(itemView.context).load(match.鞋子圖片網址).into(shoesImageView)

                itemView.setOnClickListener {
                    navigateToMatchDetail(match)
                }
            }

            private fun navigateToMatchDetail(match: Match) {
                val intent = Intent(itemView.context, Matching_details::class.java)
                intent.putExtra("matchData", match)  // Assuming Match is Serializable or Parcelable
                itemView.context.startActivity(intent)
            }
        }
    }

    lateinit var Home: ImageView
    lateinit var newMatch: ImageView
    lateinit var Friend: ImageView
    lateinit var Clothes: ImageView
    lateinit var set: ImageView
    private val matchList = mutableListOf<Match>()
    private lateinit var matchRecyclerView: RecyclerView
    private lateinit var matchAdapter: MatchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_home)

        // Initialize the navigation buttons
        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        newMatch = findViewById(R.id.newMatch)
        newMatch.setOnClickListener {
            val intent2 = Intent(this, Match_home::class.java)
            startActivity(intent2)
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
            val intent = Intent(this, choose_add::class.java)
            startActivity(intent)
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }

        set = findViewById(R.id.set)
        set.setOnClickListener {
            val intent1 = Intent(this, Setting::class.java)
            startActivity(intent1)
            finish()
        }

        // Initialize PopupMenu for actions
        val menu_catch = findViewById<ImageView>(R.id.menu_catch)
        menu_catch.setOnClickListener {
            val popupMenu = PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenu), menu_catch)
            popupMenu.inflate(R.menu.menu_pop)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        Toast.makeText(this, "新增搭配", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, New_Match::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.check -> {
                        Toast.makeText(this, "查看搭配", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, Match_home::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.share -> {
                        Toast.makeText(this, "分享搭配", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, share_Match::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()  // 顯示 PopupMenu
        }

        // Set up RecyclerView for displaying matches
        matchRecyclerView = findViewById(R.id.match_recycler_view)
        matchRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        matchAdapter = MatchAdapter(emptyList())
        matchRecyclerView.adapter = matchAdapter

        // Fetch the "搭配" data from Firestore
        fetchMatchDataFromFirestore()
    }

    // Fetch matching data from Firestore based on the current user's email
    private fun fetchMatchDataFromFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            val db = FirebaseFirestore.getInstance()

            if (email != null) {
                db.collection(email)
                    .get()
                    .addOnSuccessListener { documents ->
                        matchList.clear() // Clear the existing list to reload fresh data
                        if (documents.isEmpty) {
                            Log.e("Firestore", "No documents found!")
                        } else {
                            for (document in documents) {
                                val match = document.toObject(Match::class.java)
                                if (match.isValid()) {
                                    matchList.add(match)
                                } else {
                                    Log.d("MatchData", "Invalid match: $match")
                                }
                            }

                            // Update the RecyclerView with the new list of matches
                            runOnUiThread {
                                matchAdapter.updateMatches(matchList)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MatchActivity", "Error fetching data: ${e.message}")
                    }
            }
        }
    }

}
