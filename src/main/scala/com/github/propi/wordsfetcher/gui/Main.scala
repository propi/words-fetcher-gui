package com.github.propi.wordsfetcher.gui

import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{FormData, HTMLFormElement, HTMLSelectElement}
import org.scalajs.dom.{Event, document}

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 4. 11. 2018.
  */
object Main {

  private val loading: Var[Boolean] = Var(false)
  private val typ: Var[String] = Var("exact")
  private val result: Vars[js.Array[Word]] = Vars.empty

  trait MFormData extends js.Object {
    def get(x: String): String
  }

  trait Word extends js.Object {
    val word: String
    val frequency: Int
  }

  private def sendRequest(): Unit = {
    val form = document.getElementById("mform").asInstanceOf[HTMLFormElement]
    val data = new FormData(form).asInstanceOf[MFormData]
    if (typ.value == "freqa") {
      val owords = data.get("owords") == "true"
      val wdics = data.get("wdics") == "true"
      val txt = if (wdics) Globals.stripText(data.get("textfreq")) else data.get("textfreq")
      val words = if (owords) {
        txt.toLowerCase.split(' ')
          .map(_.trim)
          .filter(_.nonEmpty)
          .map(_.replaceAll("[.\\\\,/#!$\"'„“%^&*;:\\[\\]@{}=\\-_`~()]", ""))
          .filter(_.nonEmpty)
          .groupBy(x => x)
          .mapValues(_.length).toList
          .sortBy(_._2)(implicitly[Ordering[Int]].reverse)
          .map(x => js.Array(js.Dynamic.literal(word = x._1, frequency = x._2).asInstanceOf[Word]))
      } else {
        txt.toLowerCase.replaceAll("\\s+", " ").toCharArray
          .groupBy(x => x)
          .mapValues(_.length)
          .toList
          .sortBy(_._2)(implicitly[Ordering[Int]].reverse)
          .map(x => js.Array(js.Dynamic.literal(word = x._1.toString, frequency = x._2).asInstanceOf[Word]))
      }
      result.value.clear()
      result.value ++= js.Array(words: _*)
    } else {
      loading.value = true
      val params = List(
        List("patterns" -> data.get("patterns")),
        if (typ.value == "comb") List("chars" -> data.get("chars")) else Nil,
        if (typ.value == "varcomb") {
          List(
            Some("issub" -> (if (data.get("issub") == "true") "true" else "false")),
            Some("isset" -> (if (data.get("isset") == "true") "true" else "false")),
            if (data.get("len").isEmpty) None else Some("len" -> data.get("len"))
          ).flatten
        } else {
          Nil
        },
        if (data.get("topk").isEmpty) Nil else List("topk" -> data.get("topk")),
        if (data.get("intopk").isEmpty) Nil else List("intopk" -> data.get("intopk")),
        if (data.get("minf").isEmpty) Nil else List("minf" -> data.get("minf"))
      ).flatten.map(x => x._1 + "=" + x._2).mkString("&")
      val url = s"/${typ.value}?$params"
      Endpoint.get[String](url) { response =>
        result.value.clear()
        result.value ++= JSON.parse(response.data).asInstanceOf[js.Array[js.Array[Word]]]
        loading.value = false
      }
    }
  }

  private def stopCompute(): Unit = {
    Endpoint.get[String]("/sc") { _ => }
  }

  @dom
  private def view: Binding[Div] = {
    <div class="main">
      <form id="mform" onsubmit={e: Event =>
        e.preventDefault()
        sendRequest()}>
        <select onchange={e: Event => typ.value = e.srcElement.asInstanceOf[HTMLSelectElement].value}>
          <option value="exact">Exact</option>
          <option value="comb">Combinations</option>
          <option value="varcomb">Various combinations</option>
          <option value="freqa">Frequency analysis</option>
        </select>
        <textarea name="textfreq" class={if (typ.bind == "freqa") "active" else "hidden"}></textarea>
        <label class={if (typ.bind == "freqa") "active" else "hidden"}>Words:
          <input type="checkbox" name="owords" value="true"></input>
        </label>
        <label class={if (typ.bind == "freqa") "active" else "hidden"}>Without diacritics:
          <input type="checkbox" name="wdics" value="true"></input>
        </label>
        <input type="text" name="chars" placeholder="Chars" class={if (typ.bind == "comb") "active" else "hidden"}></input>
        <input type="text" name="patterns" class={if (typ.bind == "freqa") "hidden" else "active"} placeholder="Patterns"></input>
        <label class={if (typ.bind == "varcomb") "active" else "hidden"}>Is sub:
          <input type="checkbox" name="issub" value="true"></input>
        </label>
        <label class={if (typ.bind == "varcomb") "active" else "hidden"}>Is set:
          <input type="checkbox" name="isset" value="true"></input>
        </label>
        <input class={if (typ.bind == "varcomb") "active" else "hidden"} type="number" name="len" placeholder="Length"></input>
        <input type="number" name="topk" class={if (typ.bind == "freqa") "hidden" else "active"} placeholder="Top-k"></input>
        <input type="number" name="intopk" class={if (typ.bind == "freqa") "hidden" else "active"} placeholder="In-Top-k"></input>
        <input type="number" name="minf" class={if (typ.bind == "freqa") "hidden" else "active"} placeholder="Min freq"></input>
        <button>Odeslat</button>
        <button onclick={e: Event =>
          e.preventDefault()
          e.stopPropagation()
          stopCompute()}>Stop</button>
      </form>
      <div class="result">
        <div class="status">
          {if (loading.bind) "loading..." else "ready!"}
        </div>{for (phrase <- result) yield {
        <div class="phrase">
          {for (word <- Constants(phrase: _*)) yield {
          <div class="word">
            <div class="label">
              {word.word}
            </div>
            <div class="freq">
              {word.frequency.toString}
            </div>
          </div>
        }}
        </div>
      }}
      </div>
    </div>
  }

  def main(args: Array[String]): Unit = {
    dom.render(document.getElementById("wf"), view)
  }

}
