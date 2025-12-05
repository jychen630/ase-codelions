#!/usr/bin/env python3
"""
Simple CLI client for your ASE MCP server.

Covers:
- Auth (start/status/delete/meta)
- Search & hashtags
- Analytics (top-hashtags, best-hours, summary, sentiment)
- Scheduling (schedule_tweet REST endpoint)
- Audit (recent, summary)
- MCP tools (tools/list, tools/call)
- Natural language wrapper ("nl" subcommand)
"""

import string
import argparse
import json
import os
import sys
import re
import difflib
from datetime import datetime, timedelta, timezone
from typing import List, Optional

import requests

# Configuration

BASE_URL = os.environ.get("MCP_BASE_URL", "http://localhost:8080")


def url(path: str) -> str:
  """Build full URL from base + path."""
  if path.startswith("/"):
    path = path[1:]
  return f"{BASE_URL}/{path}"


def pretty_print(obj) -> None:
  """Print JSON nicely."""
  print(json.dumps(obj, indent=2, sort_keys=False, default=str))


# Auth commands

def cmd_auth_start(args: argparse.Namespace) -> None:
  r = requests.get(url("/auth/start"), params={"accountId": args.account})
  r.raise_for_status()
  data = r.json()
  pretty_print(data)
  print("\nOpen this URL in your browser to authorize:")
  print(data["authorize_url"])


def cmd_auth_status(args: argparse.Namespace) -> None:
  r = requests.get(url("/auth/status"), params={"accountId": args.account})
  r.raise_for_status()
  pretty_print(r.json())


def cmd_auth_delete(args: argparse.Namespace) -> None:
  r = requests.delete(url("/auth/token"), params={"accountId": args.account})
  r.raise_for_status()
  pretty_print(r.json())


def cmd_auth_meta(_args: argparse.Namespace) -> None:
  r = requests.get(url("/auth/meta"))
  r.raise_for_status()
  pretty_print(r.json())


# Search commands

def cmd_search(args: argparse.Namespace) -> None:
  params = {
      "accountId": args.account,
      "q": args.query,
      "limit": args.limit,
  }
  if args.offset is not None:
    params["offset"] = args.offset
  r = requests.get(url("/search"), params=params)
  r.raise_for_status()
  pretty_print(r.json())


def cmd_hashtags(args: argparse.Namespace) -> None:
  params = {
      "accountId": args.account,
      "q": args.query,
      "limit": args.limit,
  }
  r = requests.get(url("/search/hashtags"), params=params)
  r.raise_for_status()
  pretty_print(r.json())


# Post management commands

def cmd_delete_post(args: argparse.Namespace) -> None:
  r = requests.delete(url(f"posts/{args.status_id}"), params={"accountId": args.account})
  r.raise_for_status()
  pretty_print(r.json())


def cmd_edit_post(args: argparse.Namespace) -> None:
  body = {"text": args.text}
  r = requests.put(url(f"posts/{args.status_id}"), params={"accountId": args.account}, json=body)
  r.raise_for_status()
  pretty_print(r.json())


# Analytics commands

def cmd_analytics_top_tags(args: argparse.Namespace) -> None:
  params = {
      "accountId": args.account,
      "n": args.n,
  }
  r = requests.get(url("/analytics/top-hashtags"), params=params)
  r.raise_for_status()
  pretty_print(r.json())


def cmd_analytics_best_hours(args: argparse.Namespace) -> None:
  params = {"accountId": args.account}
  r = requests.get(url("/analytics/best-hours"), params=params)
  r.raise_for_status()
  pretty_print(r.json())


def cmd_analytics_summary(args: argparse.Namespace) -> None:
  params = {"accountId": args.account}
  r = requests.get(url("/analytics/summary"), params=params)
  r.raise_for_status()
  pretty_print(r.json())


def cmd_analytics_sentiment(args: argparse.Namespace) -> None:
  params = {"accountId": args.account}
  r = requests.get(url("/analytics/sentiment"), params=params)
  r.raise_for_status()
  pretty_print(r.json())


# Scheduling (REST endpoint for schedule_tweet)

def cmd_schedule_tweet(args: argparse.Namespace) -> None:
  # If user passes --in-seconds / --in-minutes, compute ISO time in UTC
  if args.time:
    scheduled_time = args.time
  else:
    delta = timedelta(
        seconds=args.in_seconds or 0,
        minutes=args.in_minutes or 0,
    )
    future = datetime.now(timezone.utc) + delta
    scheduled_time = future.isoformat(timespec="seconds").replace("+00:00", "Z")

  payload = {
      "tool": "schedule_tweet",
      "params": {
          "accountId": args.account,
          "text": args.text,
          "time": scheduled_time,
      },
  }

  r = requests.post(
      url("/tools/schedule_tweet"),
      headers={"Content-Type": "application/json"},
      data=json.dumps(payload),
  )
  r.raise_for_status()
  pretty_print(r.json())



# Audit commands

def cmd_audit_recent(args: argparse.Namespace) -> None:
  r = requests.get(url("/audit/recent"), params={"limit": args.limit})
  r.raise_for_status()
  pretty_print(r.json())


def cmd_audit_summary(args: argparse.Namespace) -> None:
  r = requests.get(url("/audit/summary"), params={"hours": args.hours})
  r.raise_for_status()
  pretty_print(r.json())


# MCP generic commands

def cmd_mcp_list(_args: argparse.Namespace) -> None:
  payload = {
      "jsonrpc": "2.0",
      "id": 1,
      "method": "tools/list",
  }
  r = requests.post(
      url("/mcp"),
      headers={"Content-Type": "application/json"},
      data=json.dumps(payload),
  )
  r.raise_for_status()
  pretty_print(r.json())


def cmd_mcp_call(args: argparse.Namespace) -> None:
  # arguments come as JSON from CLI
  try:
    arguments = json.loads(args.arguments)
  except json.JSONDecodeError as e:
    print(f"Invalid JSON for --arguments: {e}", file=sys.stderr)
    sys.exit(1)

  payload = {
      "jsonrpc": "2.0",
      "id": 1,
      "method": "tools/call",
      "params": {
          "name": args.name,
          "arguments": arguments,
      },
  }
  r = requests.post(
      url("/mcp"),
      headers={"Content-Type": "application/json"},
      data=json.dumps(payload),
  )
  r.raise_for_status()
  pretty_print(r.json())


# Natural Language Command Interpreter (no heavy deps, fuzzy helpers)

NUMBER_WORDS = {
  "zero": 0,
  "one": 1,
  "two": 2,
  "three": 3,
  "four": 4,
  "five": 5,
  "six": 6,
  "seven": 7,
  "eight": 8,
  "nine": 9,
  "ten": 10,
}


def _fuzzy_match(word: str, targets: List[str], cutoff: float = 0.8) -> bool:
  """Return True if 'word' is close to any target (for typos like 'minuete')."""
  word = word.lower()
  targets = [t.lower() for t in targets]
  for tgt in targets:
    if word == tgt:
      return True
    if difflib.SequenceMatcher(None, word, tgt).ratio() >= cutoff:
      return True
  return False


def _simple_tokens(text: str) -> List[str]:
  """Rudimentary tokenization: words and punctuation."""
  return re.findall(r"\w+|[^\w\s]", text, re.UNICODE)


def _has_word_like(text: str, targets: List[str], cutoff: float = 0.8) -> bool:
  tokens = [t.lower() for t in _simple_tokens(text)]
  for tok in tokens:
    if _fuzzy_match(tok, targets, cutoff=cutoff):
      return True
  return False


def _first_number(text: str) -> Optional[int]:
  """Find first integer in the text, including 'one', 'two', etc."""
  for tok in _simple_tokens(text):
    t = tok.lower()
    if t.isdigit():
      return int(t)
    if t in NUMBER_WORDS:
      return NUMBER_WORDS[t]
  return None


def _split_sentences(text: str) -> List[str]:
  """Very simple sentence splitter on .?!"""
  parts = re.split(r'(?<=[.!?])\s+', text.strip())
  return [p for p in parts if p]


def _extract_after_keyword(text: str, keyword: str) -> Optional[str]:
  """Return substring after the first occurrence of keyword (case-insensitive)."""
  lower = text.lower()
  k = keyword.lower()
  if k not in lower:
    return None
  idx = lower.index(k) + len(k)
  return text[idx:].strip()


def _extract_account(_doc_unused, original_text: str, default: str = "test-account") -> str:
  """
  Extract account id after the word 'account' from the original text.

  Examples:
    "for account test-account"  -> "test-account"
    "account demo1."            -> "demo1"
    "account test-account."     -> "test-account"
  """
  lower = original_text.lower()
  key = "account"
  if key not in lower:
    return default

  idx = lower.index(key) + len(key)
  tail = original_text[idx:].strip()
  if not tail:
    return default

  # take up to first whitespace as account id
  raw = tail.split()[0]

  # strip punctuation like .,!? from both ends
  cleaned = raw.strip(string.punctuation)

  return cleaned or default


def interpret_nl_command(text: str):
  """
  Convert a natural language instruction into a CLI-equivalent arg list.

  Returns:
      (args, error_msg)

      - args: list of CLI-style args, e.g.
        ["schedule-tweet", "--account", "test-account", "--text", "hello", "--in-minutes", "2"]
      - error_msg: human-readable message if we understood the intent
        but are missing required info (e.g. tweet text, search query)

      If we can't understand the intent at all: (None, None).
  """
  original = text.strip()
  if not original:
    return None, None

  lower = original.lower()

  # 0. Helper: default account

  account = _extract_account(None, original, default="test-account")

  # Auth: start login / status / meta / delete token

  # Start OAuth / login
  if any(phrase in lower for phrase in [
      "log in",
      "log me in",
      "login",
      "sign in",
      "sign me in",
      "start login",
      "start auth",
      "start oauth",
  ]):
    return ["auth-start", "--account", account], None

  # Auth status
  if ("status" in lower and ("auth" in lower or "token" in lower)) or "am i logged in" in lower:
    return ["auth-status", "--account", account], None

  # Auth metadata
  if "auth meta" in lower or "auth config" in lower or "auth info" in lower:
    return ["auth-meta"], None

  # Delete / revoke token / log out
  if ("delete" in lower or "revoke" in lower or "log me out" in lower) and "token" in lower:
    acct = account
    after_for = _extract_after_keyword(original, "for")
    if after_for:
      acct = after_for.split()[0]
    return ["auth-delete", "--account", acct], None

  # 2. Scheduling: schedule-tweet (polite phrases, typos, smart message extraction)

  has_schedule = _has_word_like(original, ["schedule", "scheduled", "scheduling"])
  has_tweet = _has_word_like(original, ["tweet", "tweets"])

  if has_schedule and has_tweet:
    minutes: Optional[int] = None
    seconds: Optional[int] = None

    has_minute = _has_word_like(original, ["minute", "minutes"])
    has_second = _has_word_like(original, ["second", "seconds"])

    number = _first_number(original)

    if has_minute and number is not None:
      minutes = number
    elif has_second and number is not None:
      seconds = number


    # Figure out the tweet text
    msg: Optional[str] = None

    # If there are quotes, take what's inside the first pair
    m = re.search(r'["“](.+?)["”]', original)
    if m:
      msg = m.group(1).strip()

    # Check for 'saying', 'that says', 'says', 'say'
    if not msg:
      for kw in ["saying", "that says", "says", "say"]:
        part = _extract_after_keyword(original, kw)
        if part:
          msg = part.strip()
          break

    # If multiple sentences, assume the last sentence is the tweet text
    if not msg:
      sents = _split_sentences(original)
      if len(sents) > 1:
        msg = sents[-1].strip()

    # Fallback: everything after the word 'tweet'
    if not msg:
      part = _extract_after_keyword(original, "tweet")
      if part:
        msg = part.strip()

    # If msg is just a time phrase like "in 1 minute", treat as missing
    if msg:
      time_only_pattern = r"^in\s+\d+(\.\d+)?\s+(minute|minutes|second|seconds)\s*\.?$"
      if re.match(time_only_pattern, msg.lower()):
        msg = None

    # If still no message, ask the user instead of scheduling nonsense
    if not msg:
      error = (
        "I understood you want to schedule a tweet, but I couldn't find the tweet text.\n"
        'Please say something like: "schedule a tweet in 1 minute saying hello world".'
      )
      return None, error

    # We have a valid message now
    args: List[str] = ["schedule-tweet", "--account", account, "--text", msg]

    if minutes is not None:
      args += ["--in-minutes", str(minutes)]
    elif seconds is not None:
      args += ["--in-seconds", str(seconds)]
    else:
      # default to 1 minute if no explicit time
      args += ["--in-minutes", "1"]

    return args, None

  # 3. Analytics: sentiment, summary, top hashtags, best hours

  # Sentiment analysis
  if "sentiment" in lower:
    return ["analytics-sentiment", "--account", account], None

  # Analytics summary
  if "analytics summary" in lower or "show analytics" in lower:
    return ["analytics-summary", "--account", account], None

  # Top hashtags
  if "top hashtags" in lower or "top tags" in lower or "top hash tags" in lower:
    return ["analytics-top", "--account", account, "--n", "5"], None

  # Best posting hours
  if ("best hours" in lower or "posting hours" in lower or "best time to post" in lower):
    return ["analytics-hours", "--account", account], None


  # Search & hashtag search

  # Hashtag-focused search (require an actual hashtag)
  if "#" in original or "hashtag" in lower or "hashtags" in lower:
    q = _extract_after_keyword(original, "for")

    if not q:
      # if text contains "#something", grab the first hashtag-ish token
      m2 = re.search(r"#\w+", original)
      if m2:
        q = m2.group(0)

    if not q or "#" not in q:
      error = (
        "I understood you want to search by hashtag, but I couldn't find a hashtag.\n"
        'Please say something like: "search hashtag #databases" or "find tweets for #db".'
      )
      return None, error

    return ["hashtags", "--account", account, "--query", q.strip(), "--limit", "10"], None

  # Generic search (require some query text)
  if ("search" in lower or "find" in lower) and ("tweet" in lower or "tweets" in lower):
    lower_text = lower
    before_account_text = original
    if "account" in lower_text:
      idx = lower_text.index("account")
      before_account_text = original[:idx]

    tokens = before_account_text.split()
    q_tokens = [t for t in tokens if t.lower() not in ("search", "for", "find", "tweets", "tweet")]
    q = " ".join(q_tokens).strip()

    if not q:
      error = (
        "I understood you want to search tweets, but I couldn't find what to search for.\n"
        'Please say something like: "search tweets for hello" or "find tweets about databases".'
      )
      return None, error

    return ["search", "--account", account, "--query", q, "--limit", "10"], None

  # 4. Post management (edit/delete)

  # Delete post
  if ("delete" in lower or "remove" in lower) and ("post" in lower or "tweet" in lower or "status" in lower):
    # Try to extract status ID
    status_id = _first_number(original)
    if status_id is None:
      # Try to extract from "post 123" or "tweet 123"
      m = re.search(r"(?:post|tweet|status)\s+(\d+)", lower)
      if m:
        status_id = m.group(1)
    
    if status_id is None:
      error = (
        "I understood you want to delete a post, but I couldn't find the post ID.\n"
        'Please say something like: "delete post 123456" or "remove tweet 789".'
      )
      return None, error

    return ["delete-post", "--account", account, "--status-id", str(status_id)], None

  # Edit post
  if ("edit" in lower or "update" in lower or "change" in lower) and ("post" in lower or "tweet" in lower or "status" in lower):
    # Try to extract status ID
    status_id = _first_number(original)
    if status_id is None:
      m = re.search(r"(?:post|tweet|status)\s+(\d+)", lower)
      if m:
        status_id = m.group(1)
    
    if status_id is None:
      error = (
        "I understood you want to edit a post, but I couldn't find the post ID.\n"
        'Please say something like: "edit post 123456 saying hello world".'
      )
      return None, error

    # Extract new text
    new_text = None
    for kw in ["saying", "to", "with text", "with content"]:
      part = _extract_after_keyword(original, kw)
      if part:
        new_text = part.strip()
        break

    if not new_text:
      # Try to extract quoted text
      m = re.search(r'["\']([^"\']+)["\']', original)
      if m:
        new_text = m.group(1)

    if not new_text:
      error = (
        "I understood you want to edit a post, but I couldn't find the new text.\n"
        'Please say something like: "edit post 123456 saying hello world".'
      )
      return None, error

    return ["edit-post", "--account", account, "--status-id", str(status_id), "--text", new_text], None

  # 5. Audit

  if "audit" in lower and ("recent" in lower or "last" in lower):
    num = _first_number(original)
    limit = num if num is not None else 10
    return ["audit-recent", "--limit", str(limit)], None

  if "audit summary" in lower or "tool usage" in lower:
    num = _first_number(original)
    hours = num if num is not None else 24
    return ["audit-summary", "--hours", str(hours)], None

  # 6. MCP tools

  if "list tools" in lower or "show tools" in lower or "available tools" in lower:
    return ["mcp-list"], None

  if "call tool" in lower and "{" in original and "}" in original:
    after = _extract_after_keyword(original, "call tool")
    if after:
      parts = after.strip().split(None, 1)
      tool_name = parts[0]
      json_part = original[original.index("{") : original.rindex("}") + 1]
      return ["mcp-call", "--name", tool_name, "--arguments", json_part], None

  # 7. Fallback: generic analytics summary?
  if "summary" in lower:
    return ["analytics-summary", "--account", account], None

  # Intent not recognized
  return None, None


def cmd_nlp(args: argparse.Namespace) -> None:
  interpreted, error_msg = interpret_nl_command(args.text)

  if not interpreted:
    if error_msg:
      print(f"{error_msg}")
    else:
      print("Could not understand that instruction.")
    return

  print(" Interpreted as:", interpreted)
  # Recursively call main() with new args
  main(interpreted)


# Argument parsing


def build_parser() -> argparse.ArgumentParser:
  parser = argparse.ArgumentParser(
      description="CLI client for ASE MCP server"
  )
  parser.add_argument(
      "--base-url",
      default=BASE_URL,
      help=f"Base URL for server (default: {BASE_URL})",
  )

  sub = parser.add_subparsers(dest="command", required=True)

  # Auth
  p_auth_start = sub.add_parser("auth-start", help="Start OAuth login")
  p_auth_start.add_argument("--account", required=True, help="Logical accountId")
  p_auth_start.set_defaults(func=cmd_auth_start)

  p_auth_status = sub.add_parser("auth-status", help="Show token status")
  p_auth_status.add_argument("--account", required=True)
  p_auth_status.set_defaults(func=cmd_auth_status)

  p_auth_delete = sub.add_parser("auth-delete", help="Delete token for account")
  p_auth_delete.add_argument("--account", required=True)
  p_auth_delete.set_defaults(func=cmd_auth_delete)

  p_auth_meta = sub.add_parser("auth-meta", help="Show auth provider metadata")
  p_auth_meta.set_defaults(func=cmd_auth_meta)

  # Search
  p_search = sub.add_parser("search", help="Keyword search")
  p_search.add_argument("--account", required=True)
  p_search.add_argument("--query", "-q", required=True)
  p_search.add_argument("--limit", "-n", type=int, default=10)
  p_search.add_argument("--offset", type=int)
  p_search.set_defaults(func=cmd_search)

  p_hash = sub.add_parser("hashtags", help="Hashtag search")
  p_hash.add_argument("--account", required=True)
  p_hash.add_argument("--query", "-q", required=True,
                      help='Hashtag, e.g. "#db"')
  p_hash.add_argument("--limit", "-n", type=int, default=10)
  p_hash.set_defaults(func=cmd_hashtags)

  # Analytics
  p_top = sub.add_parser("analytics-top", help="Top hashtags")
  p_top.add_argument("--account", required=True)
  p_top.add_argument("--n", type=int, default=5)
  p_top.set_defaults(func=cmd_analytics_top_tags)

  p_best = sub.add_parser("analytics-hours", help="Best posting hours")
  p_best.add_argument("--account", required=True)
  p_best.set_defaults(func=cmd_analytics_best_hours)

  p_sum = sub.add_parser("analytics-summary", help="Analytics summary")
  p_sum.add_argument("--account", required=True)
  p_sum.set_defaults(func=cmd_analytics_summary)

  p_sent = sub.add_parser("analytics-sentiment", help="Sentiment summary")
  p_sent.add_argument("--account", required=True)
  p_sent.set_defaults(func=cmd_analytics_sentiment)

  # Scheduling
  p_sched = sub.add_parser("schedule-tweet", help="Schedule a tweet")
  p_sched.add_argument("--account", required=True)
  p_sched.add_argument("--text", required=True)
  time_group = p_sched.add_mutually_exclusive_group(required=False)
  time_group.add_argument(
      "--time",
      help="Exact ISO-8601 time in UTC, e.g. 2025-11-15T19:30:00Z",
  )
  time_group.add_argument(
      "--in-seconds",
      type=int,
      help="Schedule this many seconds from now",
  )
  time_group.add_argument(
      "--in-minutes",
      type=int,
      help="Schedule this many minutes from now",
  )
  p_sched.set_defaults(func=cmd_schedule_tweet)

  # Audit
  p_recent = sub.add_parser("audit-recent", help="Recent tool call audit rows")
  p_recent.add_argument("--limit", type=int, default=10)
  p_recent.set_defaults(func=cmd_audit_recent)

  p_asum = sub.add_parser("audit-summary", help="Tool-level summary")
  p_asum.add_argument("--hours", type=int, default=24)
  p_asum.set_defaults(func=cmd_audit_summary)

  # Post management
  p_delete_post = sub.add_parser("delete-post", help="Delete a post by ID")
  p_delete_post.add_argument("--account", required=True)
  p_delete_post.add_argument("--status-id", required=True, help="Mastodon status ID")
  p_delete_post.set_defaults(func=cmd_delete_post)

  p_edit_post = sub.add_parser("edit-post", help="Edit a post by ID")
  p_edit_post.add_argument("--account", required=True)
  p_edit_post.add_argument("--status-id", required=True, help="Mastodon status ID")
  p_edit_post.add_argument("--text", required=True, help="New post content")
  p_edit_post.set_defaults(func=cmd_edit_post)

  # MCP generic
  p_list = sub.add_parser("mcp-list", help="List MCP tools")
  p_list.set_defaults(func=cmd_mcp_list)

  p_call = sub.add_parser("mcp-call", help="Call MCP tool generically")
  p_call.add_argument("--name", required=True, help="Tool name")
  p_call.add_argument(
      "--arguments",
      required=True,
      help='JSON object with arguments, e.g. \'{"accountId":"test-account","q":"hello","limit":5}\'',
  )
  p_call.set_defaults(func=cmd_mcp_call)

  # Natural language
  p_nlp = sub.add_parser("nl", help="Natural language instruction")
  p_nlp.add_argument(
      "text",
      help='Instruction, e.g. "schedule a tweet in 2 minutes saying hello"',
  )
  p_nlp.set_defaults(func=cmd_nlp)

  return parser


def main(argv: Optional[List[str]] = None) -> None:
  parser = build_parser()
  args = parser.parse_args(argv)

  # update base url globally if overridden
  global BASE_URL
  BASE_URL = args.base_url

  try:
    args.func(args)
  except requests.HTTPError as e:
    print(f"HTTP error: {e.response.status_code} {e.response.text}", file=sys.stderr)
    sys.exit(1)
  except requests.RequestException as e:
    print(f"Request error: {e}", file=sys.stderr)
    sys.exit(1)


if __name__ == "__main__":
  main()

